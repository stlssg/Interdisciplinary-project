import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from MyMQTT import MyMQTT as mqtt
import time
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore

global db

# connot expose them to public, this is an example
username = 'xxx@xxx.com'
password = '...'
broker = 'broker.emqx.io' #'test.mosquitto.org'
port = 1883
topic = 'POLITO_ICT4SS_IP/smartPresenceApp/forgetPassword' 

# function for sending the email
def send_email(
    text = 'email body',
    subject = 'Forget Password Feedback',
    from_email = 'Smart Presence <ip2021polito@gmail.com>',
    to_emails = None
):
    
    assert isinstance(to_emails, list)
    
    msg = MIMEMultipart('alternative')
    msg['From'] = from_email
    msg['To'] = ", ".join(to_emails)
    msg['subject'] = subject
    
    txt_part = MIMEText(text, 'plain')
    msg.attach(txt_part)
    
    # html_part = MIMEText("<h1>This is working</h1>", 'html')
    # msg.attach(html_part)
    
    msg_str = msg.as_string()
    server = smtplib.SMTP(host = 'smtp.gmail.com', port = 587)
    server.ehlo()
    server.starttls()
    server.login(username, password)
    server.sendmail(from_email, to_emails, msg_str)
    
    server.quit()

# perform action upon reciving a mqtt requirement    
class SubsriberForForgetPasswordMessage():
    def __init__(self, clientID):
        self.client=mqtt(clientID, broker, port, self)
        self.topic=topic
    
    def start(self):
        self.client.start()
        self.client.mySubscribe(self.topic)

    def stop(self):
        self.client.stop()

    def notify(self,topic,msg):
        tartget_email = msg.decode("utf-8") 
        print(f'The recieved message is: {tartget_email}')
        
        user_collection = db.collection(u'RegisteredUser')
        docs = user_collection.where(u'email', u'==', tartget_email).stream()
        for doc in docs:
            password_output = doc.to_dict()['password']
        send_email(text = f'Your password is: {password_output}', to_emails = [tartget_email])
    
if __name__=="__main__":
    cred = credentials.Certificate("./myFirestoreKey.json")
    firebase_admin.initialize_app(cred)
    
    db = firestore.client()
    
    # user_collection = db.collection(u'RegisteredUser')
    # docs = user_collection.where(u'email', u'==', u'123@qwe.com').stream()
    # for doc in docs:
    #     print(f'{doc.id} => {doc.to_dict()}')
    
    # start to listen to mqtt message    
    mySubsriberForForgetPasswordMessage = SubsriberForForgetPasswordMessage('myGmailSenderSmartPresencePolito-287288')
    mySubsriberForForgetPasswordMessage.start()
    while True:
        time.sleep(3)
    mySubsriberForForgetPasswordMessage.stop()