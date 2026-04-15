
#!/bin/bash

# Get the script directory and project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Load environment variables from .env file
if [ -f "$PROJECT_ROOT/.env" ]; then
    set -a  # Automatically export all variables
    source "$PROJECT_ROOT/.env"
    set +a
else
    echo "Warning: .env file not found at $PROJECT_ROOT/.env"
fi

# Generate unique email with timestamp
TIMESTAMP=$(date +%Y%m%d%H%M%S)

./seed-therapist-and-client.sh \
     --therapist-name "Jane Doe" \
     --therapist-email "jane.${TIMESTAMP}@clinic.com" \
     --therapist-specialization "Anxiety Disorders" \
     --client-name "Ivan Petrenko" \
     --client-email "ivan.${TIMESTAMP}@example.com"

./seed-therapist-and-client.sh \
     --therapist-name "th1" \
     --therapist-email "alphanet.vin1@gmail.com" \
     --therapist-specialization "Anxiety Disorders" \
     --client-name "Ivan Petrenko 1" \
     --client-email "ivan.${TIMESTAMP}@example.com"     