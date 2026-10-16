"""
Cloud Function: order-notifications
Triggered by Pub/Sub topic 'order-notifications'.
Decodes the message and sends an email via Gmail SMTP.
"""

import base64
import json
import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

from google.cloud import secretmanager


def get_secret(project_id: str, secret_name: str) -> str:
    client = secretmanager.SecretManagerServiceClient()
    name = f"projects/{project_id}/secrets/{secret_name}/versions/latest"
    response = client.access_secret_version(request={"name": name})
    return response.payload.data.decode("UTF-8")


def send_email(sender: str, app_password: str, recipient: str, subject: str, body: str):
    msg = MIMEMultipart("alternative")
    msg["Subject"] = subject
    msg["From"] = sender
    msg["To"] = recipient

    msg.attach(MIMEText(body, "plain"))

    with smtplib.SMTP("smtp.gmail.com", 587) as server:
        server.ehlo()
        server.starttls()
        server.login(sender, app_password)
        server.sendmail(sender, recipient, msg.as_string())


def notify_order(event, context):
    """
    Entry point for Cloud Function.
    event['data'] is a base64-encoded JSON Pub/Sub message.
    """
    project_id = os.environ.get("GCP_PROJECT")

    # Decode Pub/Sub message
    if "data" in event:
        raw = base64.b64decode(event["data"]).decode("utf-8")
    else:
        print("No data in event, skipping.")
        return

    try:
        payload = json.loads(raw)
    except json.JSONDecodeError:
        payload = {"message": raw}

    print(f"Received notification payload: {payload}")

    # Fetch secrets
    sender_email = get_secret(project_id, "NOTIFICATION_SENDER_EMAIL")
    app_password = get_secret(project_id, "SENDGRID_API_KEY")  # stores Gmail App Password

    # Recipient: use payload field or fall back to sender (self-notification)
    recipient = payload.get("customerEmail") or payload.get("email") or sender_email

    # Build email content
    order_id = payload.get("orderId", "N/A")
    item = payload.get("item", payload.get("productId", "your item"))
    quantity = payload.get("quantity", "")
    status = payload.get("status", "processed")

    subject = f"Flash Sale Order Confirmation — Order #{order_id}"
    body = (
        f"Hi,\n\n"
        f"Your flash sale order has been {status}!\n\n"
        f"Order ID : {order_id}\n"
        f"Item     : {item}\n"
    )
    if quantity:
        body += f"Quantity : {quantity}\n"
    body += (
        f"\nThank you for shopping with us.\n\n"
        f"— Flash Sale Team\n"
    )

    send_email(sender_email, app_password, recipient, subject, body)
    print(f"Email sent to {recipient} for order {order_id}")
