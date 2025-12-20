import base64
import json
import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

def send_email_pubsub(event, context):
    try:
        if 'data' in event:
            pubsub_message = base64.b64decode(event['data']).decode('utf-8')
            message_data = json.loads(pubsub_message)
        else:
            print("Missing data in message")
            return

        print(f"Received send request: {message_data}")

        recipient = message_data.get('to')
        subject = message_data.get('subject')
        body = message_data.get('body')

        if not recipient or not body:
            print("Missing content in request")
            return

        smtp_server = "smtp-relay.brevo.com"
        smtp_port = 587
        smtp_user = os.environ.get('SMTP_USER')
        smtp_pass = os.environ.get('SMTP_PASS')
        smtp_sender = os.environ.get('SMTP_SENDER')

        if not smtp_user or not smtp_pass:
            print("Missing SMTP configuration")
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

        print(f"Email sent to {recipient}")

    except Exception as e:
        print(f"Error sending mail: {e}")
        raise e