import logging
import json
from datetime import datetime, timezone
from confluent_kafka import Producer
from app.config import settings

logger = logging.getLogger(__name__)

_producer = None

def get_producer() -> Producer:
    """Returns a singleton Kafka producer instance."""
    global _producer
    if _producer is None:
        _producer = Producer({
            "bootstrap.servers": settings.kafka_bootstrap_servers,
            "client.id": "ai-orchestrator-producer"
        })
    return _producer

def _delivery_callback(err, msg):
    """Callback for Kafka message delivery confirmation."""
    if err:
        logger.error(f"Kafka delivery failed: {err}")
    else:
        logger.info(f"Kafka message delivered to {msg.topic()} [{msg.partition()}]")

def publish_step_completed(task_id: str, step_name: str, status: str, details: dict = None):
    """
    Publishes a StepCompletedEvent to Kafka.
    Used to notify downstream services (e.g., Notification Service) about each agent's progress.
    """
    try:
        producer = get_producer()
        event = {
            "event_type": "StepCompletedEvent",
            "task_id": task_id,
            "step_name": step_name,
            "status": status,
            "details": details or {},
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        producer.produce(
            topic="task-events",
            key=task_id,
            value=json.dumps(event),
            callback=_delivery_callback
        )
        producer.poll(0)  # Trigger delivery callbacks
        logger.info(f"Published StepCompletedEvent: task={task_id}, step={step_name}")
    except Exception as e:
        logger.error(f"Failed to publish StepCompletedEvent: {e}")

def publish_task_completed(task_id: str, status: str, generated_code: str = None, execution_passed: bool = False):
    """
    Publishes a TaskCompletedEvent to Kafka.
    Used to notify the Project Service and Notification Service that the AI workflow is done.
    """
    try:
        producer = get_producer()
        event = {
            "event_type": "TaskCompletedEvent",
            "task_id": task_id,
            "status": status,
            "generated_code": generated_code,
            "execution_passed": execution_passed,
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
        producer.produce(
            topic="task-events",
            key=task_id,
            value=json.dumps(event),
            callback=_delivery_callback
        )
        producer.flush(timeout=5)  # Ensure delivery before workflow ends
        logger.info(f"Published TaskCompletedEvent: task={task_id}, status={status}")
    except Exception as e:
        logger.error(f"Failed to publish TaskCompletedEvent: {e}")