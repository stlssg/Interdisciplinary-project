import threading
import firebase_admin
from firebase_admin import credentials, firestore
import json
import os
import downtimestamps
import occupancy
import upload
import schedule
import time
#connecting to firebase
current_path = os.path.dirname(__file__)
cred = credentials.Certificate(current_path+"/ip-polito-app-data-firebase-adminsdk-31n3r-93eb1d2eb6.json")
firebase_admin.initialize_app(cred)
db = firestore.client()

class get_database_info():
    
    def __init__(self):
        self.BuildingList=[]
        self.Building_info={}


        #read building list
    def get_building_info(self):
        users_ref = db.collection("BuildingNameList")
        docs = users_ref.stream()
        for doc in docs:
            self.BuildingList.append(doc.id)
        
        for item in self.BuildingList:
            

            
            bd_ref = db.collection(item)
            
            bdinfo = db.collection(item).document("Building_Information").get()
            bdinfo = bdinfo.to_dict()

            self.user_info={"maxusernum":int(bdinfo["Maximum_expected_number"])}
            docs = bd_ref.stream()
            i = 0
            for doc in docs:
                
                if doc.id != "Building_Information" and doc.id != "Results":
                    self.user_info.update({"user"+str(i):doc.id})
                    i += 1
                    
            self.user_info.update({"presentusernum":i})
            self.Building_info.update({item:self.user_info})
            
            
            
        print(self.Building_info)
        
        
    def get_building_occupancy(self):
 
        for building in self.Building_info:
            print(f"now processing {building}")
            #Determine whether the total number of occupants in the house is equal 
            #to the number of registered users in the building, if not, ignore this building
            if self.Building_info[building]['maxusernum'] != self.Building_info[building]['presentusernum']:
                continue
            if self.Building_info[building]['maxusernum'] == 1:
                #the case when the building has only one occupant, building occupancy = person occupancy 
                collections = db.collection(building).document(self.Building_info[building]['user0']).collections()
                for collection in collections:
                    #determain if data is recorded by positioning method 
                    if collection.id == "WIFI" or  collection.id == "POSITIONING":
                        
                        #download and save the datafile

                        a=downtimestamps.DownloadData(building,self.Building_info[building]['user0'])
                        a.download_wifidata(collection.id)
                
                        f = open("timestamps_"+self.Building_info[building]['user0'] +"_WIFI.json", 'r')   
                        content=f.read()
                        a=json.loads(content)
                        
                        #get the working interval of this user, which determains the threshold of doze mode
                        
                        working_interval = get_working_interval(self.Building_info[building]['user0'])
                        
                        #calcute occupancy and save occupancy file
                        
                        x = occupancy.rawoccupancycal_wifi(a,7,23,working_interval).cal()

                        f = open('rawoccupancy_'+building+'.json', 'w')
                        f.write(json.dumps(x))
                        f.close()
                        
                        #check the number of discarded intervals and set the FrequencyNotification state
                        
                        N = occupancy.rawoccupancycal_wifi(a,7,23,working_interval).discarded_interval_check()
                        FrequencyNotificationcheck(self.Building_info[building]['user0'],N)
                        
                

                    else:
                        #case of manual recording
                        #download and save manual data
                
                        a=downtimestamps.DownloadData(building,self.Building_info[building]['user0'])
                        a.download_inoutdata(collection.id)
                        f = open("timestamps_"+self.Building_info[building]['user0'] +"_INOUT.json", 'r')   
                        content=f.read()
                        a=json.loads(content)
                        
                        #calcute occupancy and save occupancy file
                
                        x = occupancy.rawoccupancycal_inout(a).cal()
                    

                        f = open('rawoccupancy_'+building+'.json', 'w')
                        f.write(json.dumps(x))
                        f.close()
                        
                        #check the number of discarded intervals and set the FrequencyNotification state
                        N = occupancy.rawoccupancycal_inout(a).discarded_interval_check()
                        FrequencyNotificationcheck(self.Building_info[building]['user0'],N)
                
                
                #upload occupancy data to database
                
                b=upload.dataupload(building)
                b.totalup(x)         
                
                oc = occupancy.occupancycalculate(x)
                x=oc.daily_occupancy()
                b.dailyup(x)
                f = open('daily_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                
                x=oc.weekly_occupancy()
                b.weeklyup(x)
                f = open('weekly_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                
                x=oc.workdaysandweekends_occupancy()
                b.workdayandweekendup(x)
                f = open('workdaysandweekends_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()  
                
                x=oc.monthly_occupancy()
                b.monthlyup(x)
                f = open('monthly_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                
                
                x=oc.yearly_occupancy()
                b.yearlyup(x)
                f = open('yearly_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                                  
            else:
                #the case when there are multiple occupants in the house, put the occupants in a namelist,
                #calculate their occupancy separately and then integrate them
                self.namelist = []
                for i in range(0,self.Building_info[building]['maxusernum']):
                    collections = db.collection(building).document(self.Building_info[building]['user'+str(i)]).collections()
                    for collection in collections:
                        if collection.id == "WIFI" or collection.id == "POSITIONING":
                    
                            a=downtimestamps.DownloadData(building,self.Building_info[building]['user'+str(i)])
                            a.download_wifidata(collection.id)                    
                            #downtimestamps.DownloadData(building,self.Building_info[building]['user'+str(i)]).download_wifidata()
                    
                            f = open("timestamps_"+self.Building_info[building]['user'+str(i)] +"_WIFI.json", 'r')   
                            content=f.read()
                            a=json.loads(content)
                            working_interval = get_working_interval(self.Building_info[building]['user'+str(i)])
                            x = occupancy.rawoccupancycal_wifi(a,7,23,working_interval).cal()
                            f = open('rawoccupancy_'+self.Building_info[building]['user'+str(i)]+'.json', 'w')
                            f.write(json.dumps(x))
                            f.close()
                            
                            N = occupancy.rawoccupancycal_wifi(a,7,23,working_interval).discarded_interval_check()
                            FrequencyNotificationcheck(self.Building_info[building]['user'+str(i)],N)
                    
                            self.namelist.append(self.Building_info[building]['user'+str(i)])
                        else :
                            a=downtimestamps.DownloadData(building,self.Building_info[building]['user'+str(i)])
                            a.download_inoutdata(collection.id)   
                            #downtimestamps.DownloadData(building,self.Building_info[building]['user'+i]).download_inoutdata()
                            f = open("timestamps_"+self.Building_info[building]['user'+str(i)] +"_INOUT.json", 'r')   
                            content=f.read()
                            a=json.loads(content)
                    
                            x = occupancy.rawoccupancycal_inout(a).cal()
                            f = open('rawoccupancy_'+self.Building_info[building]['user'+str(i)]+'.json', 'w')
                            f.write(json.dumps(x))
                            f.close()
                            
                            N = occupancy.rawoccupancycal_inout(a).discarded_interval_check()
                            FrequencyNotificationcheck(self.Building_info[building]['user'+str(i)],N)
                            
                            
                #aggregate the occupants' occupancy            
                y = occupancy.building_occupancy_aggregate(self.Building_info[building]['maxusernum'],self.namelist)
                x = y.building_occupancy()
                f = open('rawoccupancy_'+building+'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                b=upload.dataupload(building)
                b.totalup(x)
                
                oc = occupancy.occupancycalculate(x)
                x=oc.daily_occupancy()
                b.dailyup(x)
                f = open('daily_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                
                x=oc.weekly_occupancy()
                b.weeklyup(x)
                f = open('weekly_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                
                x=oc.workdaysandweekends_occupancy()
                b.workdayandweekendup(x)
                f = open('workdaysandweekends_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()  
                
                x=oc.monthly_occupancy()
                b.monthlyup(x)
                f = open('monthly_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                
                
                x=oc.yearly_occupancy()
                b.yearlyup(x)
                f = open('yearly_data_'+ building +'.json', 'w')
                f.write(json.dumps(x))
                f.close()
                

                
def get_working_interval(name):
    
    #read the user's working_interval(unit minute) from database, return a working_interval( in seconds, as the threshold)

        userinfo = db.collection("RegisteredUser").document(name).get()
        userinfo=userinfo.to_dict()
        print(userinfo)
        #sensitivity: setting of user, when on, perform different strategy for different working interval.
        if userinfo["sensitivity"] == "on":
            if int(userinfo["working_interval"]) < 60:
                return(5400)
            else:
                interval = int(userinfo["working_interval"])*60 + 1800
                return(interval)
        else:
            interval = int(userinfo["working_interval"])*60 + 900
            return(interval)
            
def FrequencyNotificationcheck(name,num):
    #determain if the user need FrequencyNotification, if discardedNum >5, yes
    if num >=5:
        info = {'discardedNum': str(num),
                'needFrequentNotification': 'YES'}
        db.collection("RegisteredUser").document(name).set(info,merge=True)
    else:
        info = {'discardedNum': str(num),
                'needFrequentNotification': 'NO'}
        db.collection("RegisteredUser").document(name).set(info,merge=True)        
        

    

        
              
def run():
    a=get_database_info()
    a.get_building_info()
    a.get_building_occupancy()            
            
    
    


if __name__ == '__main__':
    
    

    #run the program once a week

    schedule.every().sunday.at("23:59").do(run)
    while True:
        schedule.run_pending()
        time.sleep(30)
