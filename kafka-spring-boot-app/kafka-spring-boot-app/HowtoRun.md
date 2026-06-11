# Clean up old containers
docker-compose down -v
docker system prune -f

# Start with Bitnami images
docker-compose up -d

# Wait 10 seconds
sleep 10

# Verify it's working
docker ps

Expected output:
CONTAINER ID   IMAGE                STATUS
abc123...      bitnami/zookeeper    Up 5 seconds
def456...      bitnami/kafka        Up 3 seconds

Verify KAFKA is Ready

# Check logs
docker logs kafka-broker | tail -20

# Should see: "started (kafka.server.KafkaServer)"

# Test connection
docker exec kafka-broker kafka-broker-api-versions.sh --bootstrap-server localhost:9092

# Should return API version info (✅ means it works!)

Then Run Spring Boot

# Once Kafka is running:
mvn spring-boot:run

# In another terminal, test:
curl -X POST http://localhost:8080/api/kafka/send-claims?count=10

# Watch the logs - should see:
# "Publishing claim event"
# "Received claim from partition"
# "Successfully processed claim"
