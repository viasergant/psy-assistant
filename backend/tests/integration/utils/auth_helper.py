"""
Authentication helper utilities for integration tests.
Handles JWT token management and authenticated API requests.
"""
import requests
from typing import Optional, Dict, Any
from requests.adapters import HTTPAdapter
from urllib3.util.retry import Retry


class APIClient:
    """
    HTTP client wrapper with automatic JWT authentication and retry logic.
    """

    def __init__(self, base_url: str, timeout: int = 30):
        """
        Initialize API client.

        Args:
            base_url: Base URL of the API (e.g., http://localhost:8080)
            timeout: Default request timeout in seconds
        """
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout
        self.token: Optional[str] = None
        self.session = requests.Session()

        # Configure retry strategy for transient failures
        retry_strategy = Retry(
            total=3,
            backoff_factor=0.5,
            status_forcelist=[429, 500, 502, 503, 504],
            allowed_methods=["GET", "POST", "PUT", "PATCH", "DELETE"]
        )
        adapter = HTTPAdapter(max_retries=retry_strategy)
        self.session.mount("http://", adapter)
        self.session.mount("https://", adapter)

    def set_token(self, token: str) -> None:
        """Set JWT bearer token for authenticated requests."""
        self.token = token

    def clear_token(self) -> None:
        """Clear JWT token (logout)."""
        self.token = None

    def _build_headers(self, additional_headers: Optional[Dict[str, str]] = None) -> Dict[str, str]:
        """Build request headers with authentication and content type."""
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }

        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"

        if additional_headers:
            headers.update(additional_headers)

        return headers

    def _make_request(
        self,
        method: str,
        path: str,
        json: Optional[Dict[str, Any]] = None,
        params: Optional[Dict[str, Any]] = None,
        headers: Optional[Dict[str, str]] = None,
        expected_status: Optional[int] = None,
        allow_error: bool = False
    ) -> requests.Response:
        """
        Make HTTP request with automatic token injection.

        Args:
            method: HTTP method (GET, POST, PUT, PATCH, DELETE)
            path: API path (e.g., /api/v1/users/me)
            json: JSON request body
            params: Query parameters
            headers: Additional headers
            expected_status: Expected HTTP status code (validates response)
            allow_error: If False, raises exception on non-2xx status

        Returns:
            Response object

        Raises:
            requests.HTTPError: If response status is not 2xx and allow_error=False
        """
        url = f"{self.base_url}{path}" if path.startswith('/') else f"{self.base_url}/{path}"
        request_headers = self._build_headers(headers)

        response = self.session.request(
            method=method.upper(),
            url=url,
            json=json,
            params=params,
            headers=request_headers,
            timeout=self.timeout
        )

        # Validate expected status if provided
        if expected_status is not None and response.status_code != expected_status:
            raise AssertionError(
                f"Expected status {expected_status}, got {response.status_code}. "
                f"Response: {response.text}"
            )

        # Raise for non-2xx status unless errors are allowed
        if not allow_error:
            response.raise_for_status()

        return response

    def get(self, path: str, params: Optional[Dict[str, Any]] = None, **kwargs) -> requests.Response:
        """Make GET request."""
        return self._make_request("GET", path, params=params, **kwargs)

    def post(self, path: str, json: Optional[Dict[str, Any]] = None, **kwargs) -> requests.Response:
        """Make POST request."""
        return self._make_request("POST", path, json=json, **kwargs)

    def put(self, path: str, json: Optional[Dict[str, Any]] = None, **kwargs) -> requests.Response:
        """Make PUT request."""
        return self._make_request("PUT", path, json=json, **kwargs)

    def patch(self, path: str, json: Optional[Dict[str, Any]] = None, **kwargs) -> requests.Response:
        """Make PATCH request."""
        return self._make_request("PATCH", path, json=json, **kwargs)

    def delete(self, path: str, **kwargs) -> requests.Response:
        """Make DELETE request."""
        return self._make_request("DELETE", path, **kwargs)


def login(api_client: APIClient, email: str, password: str) -> str:
    """
    Authenticate user and return JWT access token.

    Args:
        api_client: APIClient instance
        email: User email
        password: User password

    Returns:
        JWT access token

    Raises:
        requests.HTTPError: If authentication fails
        KeyError: If response doesn't contain accessToken
    """
    response = api_client.post(
        "/api/v1/auth/login",
        json={"email": email, "password": password},
        expected_status=200
    )

    data = response.json()
    access_token = data["accessToken"]

    # Set token in client for subsequent requests
    api_client.set_token(access_token)

    return access_token


def refresh_token(api_client: APIClient, refresh_token_value: str) -> str:
    """
    Refresh JWT access token using refresh token.

    Args:
        api_client: APIClient instance
        refresh_token_value: Refresh token

    Returns:
        New JWT access token

    Raises:
        requests.HTTPError: If refresh fails
        KeyError: If response doesn't contain accessToken
    """
    response = api_client.post(
        "/api/v1/auth/refresh",
        json={"refreshToken": refresh_token_value},
        expected_status=200
    )

    data = response.json()
    new_access_token = data["accessToken"]

    # Update token in client
    api_client.set_token(new_access_token)

    return new_access_token
