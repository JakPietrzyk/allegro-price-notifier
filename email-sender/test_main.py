import unittest
from unittest.mock import patch, MagicMock, call
import base64
import json
import os

from main import send_email_pubsub


class TestSendEmailPubSub(unittest.TestCase):

    def setUp(self):
        self.valid_data = {
            "to": "receiver@example.com",
            "subject": "Test Subject",
            "body": "Treść wiadomości"
        }
        json_str = json.dumps(self.valid_data)
        data_b64 = base64.b64encode(json_str.encode('utf-8')).decode('utf-8')

        self.event = {'data': data_b64}
        self.context = {}

        self.env_vars = {
            'SMTP_USER': 'test_user',
            'SMTP_PASS': 'test_pass',
            'SMTP_SENDER': 'sender@example.com'
        }

    @patch('smtplib.SMTP')
    def test_send_email_success(self, mock_smtp_cls):
        mock_server_instance = MagicMock()
        mock_smtp_cls.return_value = mock_server_instance

        with patch.dict(os.environ, self.env_vars):
            send_email_pubsub(self.event, self.context)

        mock_smtp_cls.assert_called_with("smtp-relay.brevo.com", 587)

        mock_server_instance.starttls.assert_called_once()
        mock_server_instance.login.assert_called_with('test_user', 'test_pass')
        mock_server_instance.sendmail.assert_called_once()
        mock_server_instance.quit.assert_called_once()

        args, _ = mock_server_instance.sendmail.call_args
        self.assertEqual(args[0], 'sender@example.com')
        self.assertEqual(args[1], 'receiver@example.com')
        self.assertIn('Subject: Test Subject', args[2])

    @patch('smtplib.SMTP')
    def test_missing_data_field(self, mock_smtp_cls):
        event_empty = {}  # Pusty event

        send_email_pubsub(event_empty, self.context)

        mock_smtp_cls.assert_not_called()

    @patch('smtplib.SMTP')
    def test_missing_json_fields(self, mock_smtp_cls):
        invalid_data = {"subject": "Test", "body": "Tresc"}
        data_b64 = base64.b64encode(json.dumps(invalid_data).encode('utf-8')).decode('utf-8')
        event = {'data': data_b64}

        send_email_pubsub(event, self.context)

        mock_smtp_cls.assert_not_called()

    @patch('smtplib.SMTP')
    def test_missing_env_vars(self, mock_smtp_cls):
        with patch.dict(os.environ, {}, clear=True):
            send_email_pubsub(self.event, self.context)

        mock_smtp_cls.assert_not_called()

    @patch('smtplib.SMTP')
    def test_smtp_exception(self, mock_smtp_cls):
        mock_smtp_cls.side_effect = Exception("Connection Refused")

        with patch.dict(os.environ, self.env_vars):
            with self.assertRaises(Exception) as cm:
                send_email_pubsub(self.event, self.context)

            self.assertEqual(str(cm.exception), "Connection Refused")

    @patch('smtplib.SMTP')
    def test_invalid_json_format(self, mock_smtp_cls):
        bad_json = "To nie jest JSON".encode('utf-8')
        event = {'data': base64.b64encode(bad_json).decode('utf-8')}

        with self.assertRaises(Exception):
            send_email_pubsub(event, self.context)


if __name__ == '__main__':
    unittest.main()