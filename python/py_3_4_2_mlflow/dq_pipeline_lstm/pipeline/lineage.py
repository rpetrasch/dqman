"""
OpenLineage integration for data lineage tracking
"""

import uuid
from datetime import datetime
import requests


class LineageTracker:
    """Track data lineage using OpenLineage"""

    def __init__(self, config):
        self.url = config.OPENLINEAGE_URL
        self.namespace = config.OPENLINEAGE_NAMESPACE
        self.run_id = str(uuid.uuid4())

    def emit_event(self, job_name, event_type, inputs=None, outputs=None):
        """
        Emit OpenLineage event

        Args:
            job_name: Name of the job/step
            event_type: START, RUNNING, COMPLETE, FAIL
            inputs: List of input datasets
            outputs: List of output datasets
        """
        event = {
            "eventType": event_type,
            "eventTime": datetime.utcnow().isoformat() + "Z",
            "run": {
                "runId": self.run_id
            },
            "job": {
                "namespace": self.namespace,
                "name": job_name
            },
            "inputs": inputs or [],
            "outputs": outputs or []
        }

        try:
            # Uncomment to actually send to OpenLineage
            # requests.post(self.url, json=event)
            print(f"📊 Lineage: {job_name} - {event_type}")
        except Exception as e:
            print(f"Warning: Could not emit lineage event: {e}")

    def track_ingestion(self, customers_count, sales_count):
        """Track data ingestion step"""
        self.emit_event(
            "data_ingestion",
            "COMPLETE",
            inputs=[],
            outputs=[
                {"namespace": self.namespace, "name": "customers_raw", "facets": {"rowCount": customers_count}},
                {"namespace": self.namespace, "name": "sales_raw", "facets": {"rowCount": sales_count}}
            ]
        )

    def track_validation(self):
        """Track schema validation step"""
        self.emit_event(
            "schema_validation",
            "COMPLETE",
            inputs=[
                {"namespace": self.namespace, "name": "customers_raw"},
                {"namespace": self.namespace, "name": "sales_raw"}
            ]
        )

    def track_cleaning(self, customers_clean_count, sales_clean_count):
        """Track data cleaning step"""
        self.emit_event(
            "data_cleaning",
            "COMPLETE",
            inputs=[
                {"namespace": self.namespace, "name": "customers_raw"},
                {"namespace": self.namespace, "name": "sales_raw"}
            ],
            outputs=[
                {"namespace": self.namespace, "name": "customers_clean", "facets": {"rowCount": customers_clean_count}},
                {"namespace": self.namespace, "name": "sales_clean", "facets": {"rowCount": sales_clean_count}}
            ]
        )

    def track_training(self, n_models):
        """Track model training step"""
        self.emit_event(
            "model_training",
            "COMPLETE",
            inputs=[{"namespace": self.namespace, "name": "sales_clean"}],
            outputs=[{"namespace": self.namespace, "name": "trained_models", "facets": {"modelCount": n_models}}]
        )

    def track_anomaly_detection(self, good_count, anomaly_count):
        """Track anomaly detection step"""
        self.emit_event(
            "anomaly_detection",
            "COMPLETE",
            inputs=[
                {"namespace": self.namespace, "name": "sales_clean"},
                {"namespace": self.namespace, "name": "trained_models"}
            ],
            outputs=[
                {"namespace": self.namespace, "name": "sales_validated", "facets": {"rowCount": good_count}},
                {"namespace": self.namespace, "name": "sales_anomalies", "facets": {"rowCount": anomaly_count}}
            ]
        )