import json
import os
import sys

sys.path.append(os.getcwd())

from kafka import KafkaConsumer
from main import send_email_core, logger


def run_kafka_consumer():
    KAFKA_BOOTSTRAP_SERVERS = os.environ.get('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
    KAFKA_TOPIC = os.environ.get('KAFKA_TOPIC', 'price-notifications')
    KAFKA_GROUP_ID = os.environ.get('KAFKA_GROUP_ID', 'email-sender-local-group')

    logger.info("------------------------------------------------")
    logger.info("Starting Local Kafka Consumer")
    logger.info(f"Server: {KAFKA_BOOTSTRAP_SERVERS}")
    logger.info(f"Topic:  {KAFKA_TOPIC}")
    logger.info("------------------------------------------------")

    try:
        consumer = KafkaConsumer(
            KAFKA_TOPIC,
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            auto_offset_reset='latest',
            enable_auto_commit=True,
            group_id=KAFKA_GROUP_ID,
            value_deserializer=lambda x: json.loads(x.decode('utf-8'))
        )
    except Exception as e:
        logger.critical(f"Failed to connect to Kafka: {e}")
        return

    logger.info("Consumer connected. Waiting for messages...")

    try:
        for message in consumer:
            logger.info(f"Processing Kafka message (partition {message.partition}, offset {message.offset})")

            message_data = message.value

            try:
                send_email_core(
                    recipient=message_data.get('to'),
                    subject=message_data.get('subject'),
                    body=message_data.get('body')
                )
            except Exception as e:
                logger.error(f"Failed to process message from Kafka: {e}")

    except KeyboardInterrupt:
        logger.info("Stopping Kafka Consumer by user request...")
    finally:
        consumer.close()
        logger.info("Consumer closed.")


if __name__ == "__main__":
    required_env = ['SMTP_USER', 'SMTP_PASS', 'SMTP_SENDER']
    missing = [env for env in required_env if not os.environ.get(env)]

    if missing:
        print(f"ERROR: Missing environment variables: {', '.join(missing)}")
        print("Please export them before running the consumer.")
    else:
        run_kafka_consumer()