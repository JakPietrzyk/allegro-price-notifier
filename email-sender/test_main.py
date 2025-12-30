import unittest
from unittest.mock import patch, MagicMock
import base64
import json
import os

from main import send_email_pubsub, send_email_core


class TestSendEmailCore(unittest.TestCase):
    def setUp(self):
        self.recipient = "receiver@example.com"
        self.subject = "Test Subject"
        self.body = "Message Body"

        self.env_vars = {
            'SMTP_USER': 'test_user',
            'SMTP_PASS': 'test_pass',
            'SMTP_SENDER': 'sender@example.com'
        }

    @patch('smtplib.SMTP')
    def test_send_email_core_success(self, mock_smtp_cls):
        mock_server_instance = MagicMock()
        mock_smtp_cls.return_value = mock_server_instance

        with patch.dict(os.environ, self.env_vars):
            send_email_core(self.recipient, self.subject, self.body)

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
    def test_core_missing_content(self, mock_smtp_cls):
        send_email_core(self.recipient, self.subject, None)
        mock_smtp_cls.assert_not_called()

        send_email_core(None, self.subject, self.body)
        mock_smtp_cls.assert_not_called()

    @patch('smtplib.SMTP')
    def test_core_missing_env_vars(self, mock_smtp_cls):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(ValueError):
                send_email_core(self.recipient, self.subject, self.body)

        mock_smtp_cls.assert_not_called()

    @patch('smtplib.SMTP')
    def test_core_smtp_exception(self, mock_smtp_cls):
        mock_smtp_cls.side_effect = Exception("Connection Refused")

        with patch.dict(os.environ, self.env_vars):
            with self.assertRaises(Exception) as cm:
                send_email_core(self.recipient, self.subject, self.body)

            self.assertEqual(str(cm.exception), "Connection Refused")


class TestSendEmailPubSub(unittest.TestCase):
    def setUp(self):
        self.valid_data = {
            "to": "receiver@example.com",
            "subject": "Test Subject",
            "body": "Message Body"
        }
        json_str = json.dumps(self.valid_data)
        data_b64 = base64.b64encode(json_str.encode('utf-8')).decode('utf-8')

        self.event = {'data': data_b64}
        self.context = {}

    @patch('main.send_email_core')
    def test_pubsub_decodes_and_calls_core(self, mock_core):
        send_email_pubsub(self.event, self.context)

        mock_core.assert_called_once_with(
            recipient="receiver@example.com",
            subject="Test Subject",
            body="Message Body"
        )

    @patch('main.send_email_core')
    def test_pubsub_missing_data_field(self, mock_core):
        event_empty = {}
        send_email_pubsub(event_empty, self.context)
        mock_core.assert_not_called()

    @patch('main.send_email_core')
    def test_pubsub_invalid_json(self, mock_core):
        bad_json = "To nie jest JSON".encode('utf-8')
        event = {'data': base64.b64encode(bad_json).decode('utf-8')}

        with self.assertRaises(Exception):
            send_email_pubsub(event, self.context)

        mock_core.assert_not_called()

    @patch('main.send_email_core')
    def test_pubsub_propagates_core_exception(self, mock_core):
        mock_core.side_effect = Exception("Core Error")

        with self.assertRaises(Exception) as cm:
            send_email_pubsub(self.event, self.context)

        self.assertEqual(str(cm.exception), "Core Error")


if __name__ == '__main__':
    unittest.main()