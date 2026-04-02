
# Generate unique email with timestamp
TIMESTAMP=$(date +%Y%m%d%H%M%S)

./seed-therapist-and-client.sh \
     --therapist-name "Jane Doe" \
     --therapist-email "jane.${TIMESTAMP}@clinic.com" \
     --therapist-specialization "Anxiety Disorders" \
     --client-name "Ivan Petrenko" \
     --client-email "ivan.${TIMESTAMP}@example.com"