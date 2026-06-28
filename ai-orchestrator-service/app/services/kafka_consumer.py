import logging
import json
import threading
import asyncio
from confluent_kafka import Consumer, KafkaError
from app.config import settings

logger = logging.getLogger(__name__)

class TaskEventConsumer:
    """
    Kafka consumer that listens for TaskCreatedEvent messages
    and triggers the LangGraph AI workflow automatically.
    """

    def __init__(self, workflow_callback):
        """
        Args:
            workflow_callback: An async function that accepts task_data dict
                               and runs the AI workflow.
        """
        self._workflow_callback = workflow_callback
        self._running = False
        self._thread = None
        self._consumer = None

    def start(self):
        """Starts the Kafka consumer in a background thread."""
        if self._running:
            logger.warning("Kafka consumer is already running.")
            return

        self._running = True
        self._thread = threading.Thread(target=self._consume_loop, daemon=True)
        self._thread.start()
        logger.info("Kafka consumer started — listening on topic 'task-events'")

    def stop(self):
        """Gracefully stops the Kafka consumer."""
        self._running = False
        if self._thread:
            self._thread.join(timeout=10)
        if self._consumer:
            self._consumer.close()
        logger.info("Kafka consumer stopped.")

    def _consume_loop(self):
        """Main consumer loop running in a background thread."""
        try:
            self._consumer = Consumer({
                "bootstrap.servers": settings.kafka_bootstrap_servers,
                "group.id": "ai-orchestrator-group",
                "auto.offset.reset": "latest",
                "enable.auto.commit": True
            })
            self._consumer.subscribe(["task-events"])

            while self._running:
                msg = self._consumer.poll(timeout=1.0)

                if msg is None:
                    continue
                if msg.error():
                    if msg.error().code() == KafkaError._PARTITION_EOF:
                        continue
                    logger.error(f"Kafka consumer error: {msg.error()}")
                    continue

                try:
                    value = json.loads(msg.value().decode("utf-8"))
                    event_type = value.get("event_type", "")

                    if event_type == "TaskCreatedEvent":
                        logger.info(f"Received TaskCreatedEvent: task_id={value.get('task_id')}")
                        task_data = {
                            "task_id": value.get("task_id"),
                            "repository_id": value.get("repository_id"),
                            "title": value.get("title"),
                            "description": value.get("description")
                        }
                        # Run the async workflow from this sync thread
                        loop = asyncio.new_event_loop()
                        loop.run_until_complete(self._workflow_callback(task_data))
                        loop.close()
                    else:
                        # Ignore events we don't care about (e.g., our own StepCompleted/TaskCompleted)
                        logger.debug(f"Ignoring event: {event_type}")

                except json.JSONDecodeError:
                    logger.error(f"Failed to decode Kafka message: {msg.value()}")
                except Exception as e:
                    logger.error(f"Error processing Kafka message: {e}")

        except Exception as e:
            logger.error(f"Kafka consumer loop crashed: {e}")
        finally:
            if self._consumer:
                self._consumer.close()