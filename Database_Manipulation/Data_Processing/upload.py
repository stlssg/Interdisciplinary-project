# -*- coding: utf-8 -*-
"""
code for upload data to firebase
"""
import firebase_admin
from firebase_admin import credentials, firestore
import json
import os

current_path = os.path.dirname(__file__)

cred = credentials.Certificate(current_path+"/ip-polito-app-data-firebase-adminsdk-31n3r-93eb1d2eb6.json")
#firebase_admin.initialize_app(cred)
db = firestore.client()

class dataupload():
    def __init__(self,building_name):
        self.building_name = building_name
        self.someone = db.collection(self.building_name).document("Results")

        self.someone.set({'daily_available': True,
                             "monthly_12_available":True,
                             "monthly_available":False,
                             "total_available":True,
                             "weekly_available":True,
                             "yearly_available":True})
    
    
    def totalup(self,totaldata):

        self.totaldata=totaldata

        for self.time in self.totaldata:
            self.someonetotal = db.collection(self.building_name).document("Results").collection("total_data").document(self.time)
            self.hourdata = {"occupancy":self.totaldata[self.time],"interval":self.time}
            self.someonetotal.set(self.hourdata,merge=True)

    
    
    
    
    def dailyup(self,dailydata):
        

        self.dailydata=dailydata


        i=0
        for self.hour in self.dailydata:
            
            j=i+1
            self.hours=str('%02d'%i)+":00-"+str('%02d'%j)+":00"
            self.someonedaily = db.collection(self.building_name).document("Results").collection("daily_data").document(self.hours)
            self.hourdata = {"occupancy":self.dailydata[self.hour],"interval":self.hours}
            self.someonedaily.set(self.hourdata,merge=True)
            i=i+1
            
    def weeklyup(self,weeklydata):
       
        self.weeklydata = weeklydata

        for self.time in self.weeklydata:
            self.someoneweekly = db.collection(self.building_name).document("Results").collection("weekly_data").document(self.time)
            self.hourdata = {"occupancy":self.weeklydata[self.time],"interval":self.time}
            self.someoneweekly.set(self.hourdata,merge=True)
            
    def workdayandweekendup(self,workdaysandweekends_data):

        self.workdaysandweekends_data = workdaysandweekends_data

        for self.time in self.workdaysandweekends_data:
            self.someoneweekly = db.collection(self.building_name).document("Results").collection("weekdaysAndWeekends_data").document(self.time)
            self.hourdata = {"occupancy":self.workdaysandweekends_data[self.time],"interval":self.time}
            self.someoneweekly.set(self.hourdata,merge=True)

    def monthlyup(self,monthlydata):

        self.monthlydata = monthlydata
        
        for self.time in self.monthlydata:
            self.someonemonthly = db.collection(self.building_name).document("Results").collection("monthly_data").document(self.time)
            self.hourdata = {"occupancy":self.monthlydata[self.time],"interval":self.time}
            self.someonemonthly.set(self.hourdata)

    def yearlyup(self, yearlydata):

        self.yearlydata = yearlydata

        for self.time in self.yearlydata:
            self.someoneyearly = db.collection(self.building_name).document("Results").collection("yearly_data").document(self.time)
            self.hourdata = {"occupancy":self.yearlydata[self.time],"interval":self.time}
            self.someoneyearly.set(self.hourdata,merge=True)

        


if __name__ == '__main__':

    b=dataupload()
    b.dailyup()
    b.weeklyup()
    b.workdayandweekendup()
    b.totalup()
    b.monthlyup()
    b.yearlyup()


