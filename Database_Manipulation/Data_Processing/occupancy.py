import datetime
import json
import os
import statistics
import numpy as np



class building_occupancy_aggregate(): #aggregate occupancy from occupants to building
    def __init__(self,Ntenant,namelist):
        self.Ntenant = Ntenant
        self.namelist = namelist
        self.rawdata = {}
        for name in self.namelist:
            f = open('rawoccupancy_'+name+'.json', 'r')
            self.content = f.read()
            self.content=json.loads(self.content)
            self.rawdata.update({name:self.content})
            
        self.timelist=[]
        self.startdate=[]
        self.enddate=[]
    def building_occupancy(self,fmt='%Y-%m-%d'):  
        # find the earlist and latest time recorded of the occupants ,calculate occupancy during this period
        for name in self.rawdata:      #find everyone's  start time
            for time in self.rawdata[name]:
                self.timelist.append(time[0:10])
            self.startdate.append(self.timelist[0])
            self.enddate.append(self.timelist[len(self.timelist)-1])
        
            
        self.earleist_date = self.startdate[0]  #find the earlist start time
        for i in self.startdate:
            if datetime.datetime.strptime(self.earleist_date, fmt) > datetime.datetime.strptime(i, fmt) :
                self.earleist_date = i
        
        self.latest_date = self.enddate[0]        #then the latest recorded time
        for i in self.enddate:
            if datetime.datetime.strptime(self.latest_date, fmt) < datetime.datetime.strptime(i, fmt) :
                self.latest_date = i    

        #Create a dictionary that iterates over all time periods and assigns the value -1, -1 means no data 
        begin_date = datetime.datetime.strptime(self.earleist_date+'-00', "%Y-%m-%d-%H")
        end_date = datetime.datetime.strptime(self.latest_date+'-23',"%Y-%m-%d-%H")
        self.time_period={}        
        while begin_date <= end_date:
            
            date_str = begin_date.strftime("%Y-%m-%d-%H")
            self.time_period.update({date_str:-1})
            begin_date += datetime.timedelta(hours=1)
            
        self.align_rawdata={}    
        for name in self.rawdata:  
            self.align_rawdata.update({name:self.time_period})
        #Regenerate a dictionary to prevent memory address conflicts
        self.abcd=json.dumps(self.align_rawdata)
        self.align_rawdata=json.loads(self.abcd)


        #Copies all single-occupant data to this collection so that all occupant data have the same start and end date
        
        for key in self.align_rawdata.keys():
            self.align_rawdata[key].update(self.rawdata[key])  
        

                        
        #Calculate the occupancy of each day, and only average the time period when multiple people are not -1
        
        self.building_average_occupancy={}

        for time in self.time_period:
            templist=[]
            for name in self.align_rawdata:
                templist.append(self.align_rawdata[name][time])


            temp=[]
            for value in templist:
                if value !=-1:
                    temp.append(value)
            if len(temp) == self.Ntenant:
               
                self.building_average_occupancy.update({time:np.mean(temp)})
            else:
                self.building_average_occupancy.update({time:-1})
                

        
        return(self.building_average_occupancy)

class rawoccupancycal_inout():
    # function of processing manual recording data to single-user occupancy
    def __init__(self,timestamp):
        self.timestamp = timestamp

        self.occupancydict={}    
        self.t=[]
        self.inoutlist = []
        for i in sorted (self.timestamp) :  #Sort and convert timestamps into lists

            self.t.append(i[0:16])
            self.inoutlist.append(self.timestamp[i])
            
    def cal(self):
        self.tstr1=self.t[0]
        self.tstr2=self.t[len(self.t)-1]
        self.s=int(self.tstr1[11:13])        #starting time
        self.f=int(self.tstr2[11:13])        #ending time        

            
                #Traverse the collection of all time periods and assign a value of 0
        begin_date = datetime.datetime.strptime(self.tstr1[0:10]+'-00', "%Y-%m-%d-%H")
        end_date = datetime.datetime.strptime(self.tstr2[0:10]+'-23',"%Y-%m-%d-%H")
   
        
        while begin_date <= end_date:
            
            date_str = begin_date.strftime("%Y-%m-%d-%H")
            self.occupancydict.update({date_str:0})
            begin_date += datetime.timedelta(hours=1)
            
            
        #Set the part before and after the record to -1, make everyday meet 24 hours
        for i in range(0,self.s):                 
            self.occupancydict[self.tstr1[0:10]+'-'+str("%02d"%i)]=-1

        for i in range(self.f+1,24):                      
            self.occupancydict[self.tstr2[0:10]+'-'+str("%02d"%i)]=-1        
            
            
        self.invaliddict ={} 
        for i in range(0,len(self.t)-1):                #traverse every timestamps
            self.tstr1=self.t[i]
            self.t1=datetime.datetime.strptime(self.tstr1, '%Y-%m-%dT%H:%M')
            self.tstr2=self.t[i+1]
            self.t2=datetime.datetime.strptime(self.tstr2, '%Y-%m-%dT%H:%M') 
            self.interval=self.t2-self.t1 
            
            #if a couple of timestamps start by in and end by out, means the user is present, calculate occupancy
            if self.inoutlist[i] == "IN" and self.inoutlist[i+1] == "OUT":

                #if the characters representing the hours are the same, means the period is less that 1 hour
                if self.tstr1[11:13]==self.tstr2[11:13]:             
                    self.occupancydict[self.tstr1[0:10]+'-'+self.tstr1[11:13]]=self.occupancydict[self.tstr1[0:10]+'-'+self.tstr1[11:13]]+self.interval.seconds/3600
                #if the characters representing the hours differ by one, calculate occupancy of the two hours
                elif int(self.tstr2[11:13])-int(self.tstr1[11:13])==1:         
                    self.tstr0=self.tstr2[0:14]+"00"
                    self.tv0=datetime.datetime.strptime(self.tstr0, '%Y-%m-%dT%H:%M')
                    timeinterval = self.tv0-self.t1
                    self.earlypart = timeinterval.seconds
                    self.laterpart = self.interval.seconds - self.earlypart
                    self.occupancydict[self.tstr1[0:10]+'-'+self.tstr1[11:13]]=self.occupancydict[self.tstr1[0:10]+'-'+self.tstr1[11:13]]+self.earlypart/3600
                    self.occupancydict[self.tstr2[0:10]+'-'+self.tstr2[11:13]]=self.occupancydict[self.tstr2[0:10]+'-'+self.tstr2[11:13]]+self.laterpart/3600
                #if the characters representing the hours differ by more than one, 
                #calculate occupancy of the starting and ending hours, fill the hours in between with 1 
                else:  
                    self.tstr0=self.tstr1[0:14]+"00"

                    temp=datetime.datetime.strptime(self.tstr0, '%Y-%m-%dT%H:%M')
                    temp += datetime.timedelta(hours=1)
                    self.tstr0=temp.strftime('%Y-%m-%dT%H:%M')
                    self.t0=datetime.datetime.strptime(self.tstr0, '%Y-%m-%dT%H:%M')
                    self.tstr00=self.tstr2[0:14]+"00"
                    self.t00=datetime.datetime.strptime(self.tstr00, '%Y-%m-%dT%H:%M')
                    startinterval = self.t0-self.t1
                    endinterval = self.t2 - self.t00
                    self.occupancydict[self.tstr1[0:10]+'-'+self.tstr1[11:13]]=self.occupancydict[self.tstr1[0:10]+'-'+self.tstr1[11:13]]+startinterval.seconds/3600
                    self.occupancydict[self.tstr2[0:10]+'-'+self.tstr2[11:13]]=self.occupancydict[self.tstr2[0:10]+'-'+self.tstr2[11:13]]+endinterval.seconds/3600
                    
                    begin_time = datetime.datetime.strptime(self.tstr0[0:13], "%Y-%m-%dT%H")
                    end_time = datetime.datetime.strptime(self.tstr00[0:13],"%Y-%m-%dT%H")
   
        
                    while begin_time < end_time:
            
                        date_str = begin_time.strftime("%Y-%m-%d-%H")
                        self.occupancydict.update({date_str:1})
                        begin_time += datetime.timedelta(hours=1)
                    
            #if a couple of timestamps start by out and end by in, means the user is not present        
            elif self.inoutlist[i] == "OUT" and self.inoutlist[i+1] == "IN":
                pass
            else:
                #Record the set of non-compliant hours and set them to -1, such as "in" and "in" couples, they are the discarded ones
                begin_time = datetime.datetime.strptime(self.tstr1[0:13], "%Y-%m-%dT%H")
                end_time = datetime.datetime.strptime(self.tstr2[0:13],"%Y-%m-%dT%H")
   
        
                while begin_time <= end_time:
            
                        date_str = begin_time.strftime("%Y-%m-%d-%H")
                        self.invaliddict.update({date_str:-1})
                        begin_time += datetime.timedelta(hours=1)
                
        self.occupancydict.update(self.invaliddict)    
            
            
        return(self.occupancydict)
            
            
    def discarded_interval_check(self):
        #determine how many discarded intervals in the recent week
        self.Ndiscarded = 0
        self.strlasttimestamp = self.t[len(self.t)-1]
        self.lasttimestamp=datetime.datetime.strptime(self.strlasttimestamp, '%Y-%m-%dT%H:%M')
        self.oneweekinterval = datetime.timedelta(days=7)
        self.oneweekago_timestamp = self.lasttimestamp - self.oneweekinterval
        #Traverse the timestamps and list the timestamps of this week separately
        self.theweek=[]
        self.inoutoftheweek=[]
        for i in range(0,len(self.t)):
            if datetime.datetime.strptime(self.t[i], '%Y-%m-%dT%H:%M')>=self.oneweekago_timestamp:
                self.theweek.append(self.t[i])
                self.inoutoftheweek.append(self.inoutlist[i])
        #if there are two adjacent timestamps having a same mark(both "in" or both "out"),mark them as discarded
        for i in range(0,len(self.theweek)-1):

            if self.inoutoftheweek[i] == self.inoutoftheweek[i+1]:       
                self.Ndiscarded += 1

        
        return(self.Ndiscarded)            
            
            
            
            
            
class rawoccupancycal_wifi():
       # function of processing data recorded by positioning method to single-user occupancy
    def __init__(self,timestamp,starttime,stoptime,working_interval):
        self.timestamp = timestamp
        self.starttime=starttime
        self.stoptime=stoptime
        self.working_interval = working_interval
        #print(self.timestamp)
        self.occupancydict={}
        self.t=[]

        for i in sorted (self.timestamp) :  ##Sort and convert timestamps into lists
   
            self.t.append(i[0:16])
  
    def discarded_interval_check(self):
        self.Ndiscarded = 0
        self.strlasttimestamp = self.t[len(self.t)-1]
        self.lasttimestamp=datetime.datetime.strptime(self.strlasttimestamp, '%Y-%m-%dT%H:%M')
        self.oneweekinterval = datetime.timedelta(days=7)
        self.oneweekago_timestamp = self.lasttimestamp - self.oneweekinterval

        self.theweek=[]
        for i in range(0,len(self.t)):
            if datetime.datetime.strptime(self.t[i], '%Y-%m-%dT%H:%M')>=self.oneweekago_timestamp:
                self.theweek.append(self.t[i])
        

        for i in range(0,len(self.theweek)-1):
            self.tstr1=self.t[i]
            self.t1=datetime.datetime.strptime(self.tstr1, '%Y-%m-%dT%H:%M')
            self.tstr2=self.t[i+1]
            self.t2=datetime.datetime.strptime(self.tstr2, '%Y-%m-%dT%H:%M')
            
            timeinterval=self.t2-self.t1         #calculate interval between two adjacent stamps
            datedelta=self.t2.date()-self.t1.date()  
            if datedelta.days==1 and int(self.tstr2[11:13])>14: #if the interval is over 24 hours , or later than 14:00 of the next day, discard them       
                self.Ndiscarded += 1
            elif timeinterval.days >=1  :                   
                self.Ndiscarded += 1
        
        return(self.Ndiscarded)
        

    def cal(self):
        

        self.tstr1=self.t[0]
   

        self.s=int(self.tstr1[11:13])        #starting time
        #set the first day's occupancy, before the starting time,to -1, after ther starting , to 0
        for i in range(0,self.s):                  
            self.occupancydict[self.tstr1[0:10]+'-'+str("%02d"%i)]=-1

        for i in range(self.s,24):                     
            self.occupancydict[self.tstr1[0:10]+'-'+str("%02d"%i)]=0

        for i in range(0,len(self.t)-1):                #Traverse the collection of all time periods and assign a value of 0
            self.tstr1=self.t[i]
            self.t1=datetime.datetime.strptime(self.tstr1, '%Y-%m-%dT%H:%M')
            self.tstr2=self.t[i+1]
            self.t2=datetime.datetime.strptime(self.tstr2, '%Y-%m-%dT%H:%M')
            
            timeinterval=self.t2-self.t1         #calculate the interval of two adjacent stamps
            datedelta=self.t2.date()-self.t1.date()  
            #print(datedelta.days)
            if datedelta.days==0:        #if in the same day
                if timeinterval.seconds<self.working_interval:      #less than threshold, calculate occupancy
                    self.validintervalcal(self.occupancydict,self.tstr1,self.tstr2,timeinterval.seconds)     
                else:                                         #lager than threshold,keep  occupancy as 0
                    pass
            
            
            elif datedelta.days==1 and int(self.tstr2[11:13])>14: # discard if later than 14:00 of nextaday        
                self.invalidintervalcal(self.occupancydict,self.tstr1,self.tstr2,datedelta)
            elif timeinterval.days >=1  :              #discard if over 24 hours        
                self.invalidintervalcal(self.occupancydict,self.tstr1,self.tstr2,datedelta)
            
                
            else:                                               #valid interval
                self.tstrstop=self.tstr1[0:11]+str("%02d"%self.stoptime)+":00"  #calculate the part before stoptime 
                self.tstop=datetime.datetime.strptime(self.tstrstop, '%Y-%m-%dT%H:%M')
                timeinterval=self.tstop-self.t1
                if timeinterval.seconds<self.working_interval:      #calculate occupancy if interval less thatn threshold
                    self.validintervalcal(self.occupancydict,self.tstr1,self.tstrstop,timeinterval.seconds)     
                else:                                         
                    pass
                
                
                for i in range(self.stoptime,24):                   #set to 1 after stoptime
                    self.occupancydict[self.tstr1[0:10]+'-'+str("%02d"%i)]=1
                


                for i in range(0,24):                   #set the occupancy of second day to 0
                    self.occupancydict[self.tstr2[0:10]+'-'+str("%02d"%i)]=0
                for i in range(0,self.starttime):                   # set to 1 before start time
                    self.occupancydict[self.tstr2[0:10]+'-'+str("%02d"%i)]=1
   
                self.tstrstart=self.tstr2[0:11]+str("%02d"%self.starttime)+":00" #calculate the part after starttime 
                self.tstart=datetime.datetime.strptime(self.tstrstart, '%Y-%m-%dT%H:%M')
                timeinterval=self.t2-self.tstart
              
                if timeinterval.seconds<self.working_interval:     
                    self.validintervalcal(self.occupancydict,self.tstrstart,self.tstr2,timeinterval.seconds)     
                else:                                     
                    pass
                
                
        self.tstrlast=self.t[len(self.t)-1]
        if int(self.tstrlast[11:13]) != 24:
            for i in range(int(self.tstrlast[11:13])+1,24): 
                self.occupancydict[self.tstrlast[0:10]+'-'+str("%02d"%i)]=-1
        
        return(self.occupancydict)


                
    def validintervalcal(self,occupancydict,str1,str2,delta):
        #if the interval is a valid period, calculate the occupancy hour by hour
        self.interval=delta

        self.str1=str1
        self.tv1=datetime.datetime.strptime(self.str1, '%Y-%m-%dT%H:%M')
        self.str2=str2
        self.tv2=datetime.datetime.strptime(self.str2, '%Y-%m-%dT%H:%M')

        if self.str1[11:13]==self.str2[11:13]:             #same hour
            occupancydict[self.str1[0:10]+'-'+self.str1[11:13]]=occupancydict[self.str1[0:10]+'-'+self.str1[11:13]]+self.interval/3600
        elif int(self.str2[11:13])-int(self.str1[11:13])==1:         #differ by 1 hour
            self.str0=str2[0:14]+"00"
            self.tv0=datetime.datetime.strptime(self.str0, '%Y-%m-%dT%H:%M')
            timeinterval = self.tv0-self.tv1
            self.earlypart = timeinterval.seconds
            self.laterpart = self.interval - self.earlypart
            occupancydict[self.str1[0:10]+'-'+self.str1[11:13]]=occupancydict[self.str1[0:10]+'-'+self.str1[11:13]]+self.earlypart/3600
            occupancydict[self.str2[0:10]+'-'+self.str2[11:13]]=occupancydict[self.str2[0:10]+'-'+self.str2[11:13]]+self.laterpart/3600
        else :                                             #differ by two hour
            self.str0=str2[0:14]+"00"
            self.tv0=datetime.datetime.strptime(self.str0, '%Y-%m-%dT%H:%M')
            timeinterval = self.tv0-self.tv1
            self.earlypart = timeinterval.seconds-3600
            self.middlepart = 3600
            self.middlehour = int(self.str1[11:13])+1
            self.middlehour = "%02d" %self.middlehour
            self.middlehour = str(self.middlehour)
            self.laterpart = self.interval - self.earlypart - self.middlepart
            occupancydict[self.str1[0:10]+'-'+self.str1[11:13]]=occupancydict[self.str1[0:10]+'-'+self.str1[11:13]]+self.earlypart/3600
            occupancydict[self.str1[0:10]+'-'+self.middlehour]=1.0
            occupancydict[self.str2[0:10]+'-'+self.str2[11:13]]=occupancydict[self.str2[0:10]+'-'+self.str2[11:13]]+self.laterpart/3600
        

    def invalidintervalcal(self,occupancydict,str1,str2,datedelta):
        #for invalid period , set the occupancy of this part to -1
        self.datedelta=datedelta
        
        self.str1=str1
        self.tv1=datetime.datetime.strptime(self.str1, '%Y-%m-%dT%H:%M')
        self.str2=str2
        self.tv2=datetime.datetime.strptime(self.str2, '%Y-%m-%dT%H:%M')

        for i in range(int(self.str1[11:13]),24):               
            occupancydict[self.tstr1[0:10]+'-'+str("%02d"%i)]=-1
            
        if self.datedelta.days>1:                                     
            for i in range(1,self.datedelta.days):
                self.idatedelta=datetime.timedelta(days=1)
                self.middleday=self.tv1+self.idatedelta
                self.strmday=self.middleday.strftime("%Y-%m-%d-%H")
                for j in range(0,24):
                    occupancydict[self.strmday[0:10]+'-'+str("%02d"%j)]=-1
                    
            
        for i in range(0,int(self.str2[11:13])+1):               
            occupancydict[self.tstr2[0:10]+'-'+str("%02d"%i)]=-1    
        for i in range(int(self.str2[11:13])+1,24):                  
            self.occupancydict[self.tstr2[0:10]+'-'+str("%02d"%i)]=0    
            
class occupancycalculate():
    #calculate daily weekly monthly occupancy with the raw occupancy calculated above
    def __init__(self,rawoccupancy):
        self.rawoccupancy = rawoccupancy

            

    

        
    def daily_occupancy(self):
        #print(type(self.rawoccupancy))
        self.daily_dict={}
        self.daily_count={}
        
        for i in range(0,24):
            j= str("%02d"%i)+":00"
            self.daily_dict.update({j:0})
            self.daily_count.update({j:0})
            
        for key in self.rawoccupancy:
            if self.rawoccupancy[key]==-1:
                continue
            '''if key[11:13]=="23":
                if self.rawoccupancy[key]!=1:
                    print(key,self.rawoccupancy[key])'''
            
            self.daily_dict[key[11:13]+":00"]=self.daily_dict[key[11:13]+":00"]+self.rawoccupancy[key]
            self.daily_count[key[11:13]+":00"]=self.daily_count[key[11:13]+":00"]+1
        for key in  self.daily_dict:
            if self.daily_count[key]==0:
                self.daily_dict[key]=-1
                continue
            self.daily_dict[key]=self.daily_dict[key]/self.daily_count[key]
        return self.daily_dict
        self.daily_dict={}
        self.daily_count={}

            
            
    def weekly_occupancy(self):      
        
        self.weekly_dict={}
        self.weekly_count={}
        self.weekdays=["1-Mon","2-Tue","3-Wed","4-Thu","5-Fri","6-Sat","7-Sun"]
        
        for self.weekday in self.weekdays:
            for i in range(0,24):
                j= self.weekday+"-"+str("%02d"%i)
                self.weekly_dict.update({j:0})
                self.weekly_count.update({j:0})
                
        for key in self.rawoccupancy:
            if self.rawoccupancy[key]==-1:
                continue  
            self.weekdayoftheday = datetime.datetime.strptime(key[0:10], "%Y-%m-%d").weekday()
            
            self.weekly_dict[self.weekdays[self.weekdayoftheday]+"-"+key[11:13]]=self.weekly_dict[self.weekdays[self.weekdayoftheday]+"-"+key[11:13]]+self.rawoccupancy[key]
            self.weekly_count[self.weekdays[self.weekdayoftheday]+"-"+key[11:13]]=self.weekly_count[self.weekdays[self.weekdayoftheday]+"-"+key[11:13]]+1
            
        for key in  self.weekly_dict:   
            if self.weekly_count[key]==0:
                self.weekly_dict[key]=-1
                continue
            self.weekly_dict[key]=self.weekly_dict[key]/self.weekly_count[key]
        
        return self.weekly_dict
    
    
    def workdaysandweekends_occupancy(self):      
        
        self.workdaysandweekends_dict={}
        self.workdaysandweekends_count={}
        self.workdaysandweekends=["weekdays","weekends"]
        
        for self.workdayorweekend in self.workdaysandweekends:
            for i in range(0,24):
                j= self.workdayorweekend+"-"+str("%02d"%i)
                self.workdaysandweekends_dict.update({j:0})
                self.workdaysandweekends_count.update({j:0})
                
        for key in self.rawoccupancy:
            if self.rawoccupancy[key]==-1:
                continue  
            self.weekdayoftheday = datetime.datetime.strptime(key[0:10], "%Y-%m-%d").weekday()
            
            if self.weekdayoftheday <5:
                self.workdaysandweekends_dict["weekdays"+"-"+key[11:13]] = self.workdaysandweekends_dict["weekdays"+"-"+key[11:13]]+self.rawoccupancy[key]
                self.workdaysandweekends_count["weekdays"+"-"+key[11:13]] = self.workdaysandweekends_count["weekdays"+"-"+key[11:13]]+1             
            
            else:
                self.workdaysandweekends_dict["weekends"+"-"+key[11:13]] = self.workdaysandweekends_dict["weekends"+"-"+key[11:13]]+self.rawoccupancy[key]
                self.workdaysandweekends_count["weekends"+"-"+key[11:13]] = self.workdaysandweekends_count["weekends"+"-"+key[11:13]]+1             
            
            
        for key in  self.workdaysandweekends_dict:   
            if self.workdaysandweekends_count[key]==0:
                self.workdaysandweekends_dict[key]=-1
                continue
            self.workdaysandweekends_dict[key]=self.workdaysandweekends_dict[key]/self.workdaysandweekends_count[key]
        
        return self.workdaysandweekends_dict


        
        
        
                
        
    def monthly_occupancy(self):              
        
        self.monthly_dict={}
        self.monthly_count={}
        months=["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"]
        for month in months:
            for i in range(0,24):
                j= month+"-"+str("%02d"%i)
                self.monthly_dict.update({j:0})
                self.monthly_count.update({j:0})        
        
        for key in self.rawoccupancy:
            if self.rawoccupancy[key]==-1:
                continue  
            self.monthnum = int(key[5:7])-1
            
            self.monthly_dict[months[self.monthnum]+"-"+key[11:13]]=self.monthly_dict[months[self.monthnum]+"-"+key[11:13]]+self.rawoccupancy[key]
            self.monthly_count[months[self.monthnum]+"-"+key[11:13]]=self.monthly_count[months[self.monthnum]+"-"+key[11:13]]+1
                    
        for key in  self.monthly_dict:   
            if self.monthly_count[key]==0:
                self.monthly_dict[key]=-1
                continue
            self.monthly_dict[key]=self.monthly_dict[key]/self.monthly_count[key]
        return self.monthly_dict        
        
        
    def yearly_occupancy(self):              
        
        self.yearly_dict={}
        self.yearly_count={}
        self.years=[]
        for key in self.rawoccupancy:
            if key[0:4] in self.years:
                continue
            self.years.append(key[0:4])
        for year in self.years:
            for i in range(0,24):
                j= year+"-"+str("%02d"%i)
                self.yearly_dict.update({j:0})
                self.yearly_count.update({j:0})        
        
        for key in self.rawoccupancy:
            if self.rawoccupancy[key]==-1:
                continue  
            
            self.yearly_dict[key[0:4]+"-"+key[11:13]]=self.yearly_dict[key[0:4]+"-"+key[11:13]]+self.rawoccupancy[key]
            self.yearly_count[key[0:4]+"-"+key[11:13]]=self.yearly_count[key[0:4]+"-"+key[11:13]]+1
                    
        for key in  self.yearly_dict:   
            if self.yearly_count[key]==0:
                self.yearly_dict[key]=-1
                continue
            self.yearly_dict[key]=self.yearly_dict[key]/self.yearly_count[key]
        return self.yearly_dict        

        
        
        
if __name__ == '__main__':

    # f = open('rawoccupancy_zhao&song.json', 'r')   
    # content=f.read()
    # a=json.loads(content)

    # x=occupancycalculate(a)
    # b=x.daily_occupancy()
    # c=x.weekly_occupancy()
    # d=x.monthly_occupancy()
    # e=x.yearly_occupancy()
    # g=x.workdaysandweekends_occupancy()

    # f = open('daily_data_zhao&song.json', 'w')
    # f.write(json.dumps(b))
    # f.close()
    # f = open('weekly_data_zhao&song.json', 'w')
    # f.write(json.dumps(c))
    # f.close()
    # f = open('monthly_data_zhao&song.json', 'w')
    # f.write(json.dumps(d))
    # f.close()
    # f = open('yearly_data_zhao&song.json', 'w')
    # f.write(json.dumps(e))
    # f.close()
    # f = open('workdaysandweekends_data_zhao&song.json', 'w')
    # f.write(json.dumps(g))
    # f.close()


    

    f = open('timestamps_wang.json', 'r')   
    content=f.read()
    a=json.loads(content)
    x=rawoccupancycal_wifi(a,7,23).cal()
    for item in x:
            if x[item]==0.9999999999999999:
                x[item]=1
            x[item]=round(x[item],2)
    print(x)
    f = open('rawoccupancy_wang.json', 'w')
    f.write(json.dumps(x))
    
    f.close()
        
    x=building_occupancy_aggregate(2).building_occupancy()
    f = open('rawoccupancy_zhao&song.json', 'w')
    f.write(json.dumps(x))
    
    f = open('timestamps_song.json', 'r')   
    content=f.read()
    a=json.loads(content)    
    x=rawoccupancycal_wifi(a,7,23).discarded_interval_check()
    print(x)
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
    