import base64
import json
import os
import smtplib
import logging
from logging.handlers import TimedRotatingFileHandler
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

LOG_DIR = 'logs'

if not os.path.exists(LOG_DIR):
    os.makedirs(LOG_DIR)

logger = logging.getLogger("EmailSender")
logger.setLevel(logging.INFO)

if not logger.handlers:
    formatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    log_file_path = os.path.join(LOG_DIR, 'app.log')

    file_handler = TimedRotatingFileHandler(log_file_path, when='d', interval=1, backupCount=30)
    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)

def send_email_pubsub(event, context):
    try:
        if 'data' in event:
            pubsub_message = base64.b64decode(event['data']).decode('utf-8')
            message_data = json.loads(pubsub_message)
        else:
            logger.warning("Missing data in message")
            return

        logger.info(f"Received send request: {message_data}")

        recipient = message_data.get('to')
        subject = message_data.get('subject')
        body = message_data.get('body')

        if not recipient or not body:
            logger.warning("Missing content in request (recipient or body)")
            return

        smtp_server = "smtp-relay.brevo.com"
        smtp_port = 587
        smtp_user = os.environ.get('SMTP_USER')
        smtp_pass = os.environ.get('SMTP_PASS')
        smtp_sender = os.environ.get('SMTP_SENDER')

        if not smtp_user or not smtp_pass:
            logger.error("Missing SMTP configuration in environment variables")
            return

        msg = MIMEMultipart()
        msg['From'] = smtp_sender
        msg['To'] = recipient
        msg['Subject'] = subject
        msg.attach(MIMEText(body, 'plain'))

        server = smtplib.SMTP(smtp_server, smtp_port)
        server.starttls()
        server.login(smtp_user, smtp_pass)
        server.sendmail(smtp_sender, recipient, msg.as_string())
        server.quit()

        logger.info(f"Email sent successfully to {recipient}")

    except Exception as e:
        logger.error(f"Error sending mail: {e}", exc_info=True)
        raise e