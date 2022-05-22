import firebase_admin
from firebase_admin import credentials, firestore
import json
import os
# code for downloading data from firebase
current_path = os.path.dirname(__file__)

cred = credentials.Certificate(current_path+"/ip-polito-app-data-firebase-adminsdk-31n3r-93eb1d2eb6.json")
#firebase_admin.initialize_app(cred)
db = firestore.client()

class DownloadData():
    def __init__(self,building_name,user_name):
        self.building_name = building_name
        self.UserName = user_name



    def download_wifidata(self,datatype):

        users_ref = db.collection(self.building_name).document(self.UserName).collection(datatype)

        docs = users_ref.stream()
        timestamps = {}
        for doc in docs:
            text = {doc.id:'connected'}

            timestamps.update(text)

        f=open("timestamps_"+self.UserName +"_WIFI.json","w")
        f.write(json.dumps(timestamps))
        print(timestamps) 

        f.close()

    def download_inoutdata(self,datatype):

        users_ref = db.collection(self.building_name).document(self.UserName).collection(datatype)

        docs = users_ref.stream()
        timestamps = {}
        for doc in docs:
            text = {doc.id:'connected'}

            timestamps.update(text)

        f=open("timestamps_"+self.UserName +"_INOUT.json","w")
        f.write(json.dumps(timestamps))
        print(timestamps) 

        f.close()

if __name__ == '__main__':
    d=DownloadData('viagermanasca27','zhao_zhiqiang').download_inoutdata()