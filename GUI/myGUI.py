import PySimpleGUI as sg
import matplotlib.pyplot as plt
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
import numpy as np
import matplotlib
import json
from datetime import date, datetime, timedelta
import string
import random
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import os, sys
import calendar
from besos import eppy_funcs as ef
from besos.evaluator import EvaluatorEP
from besos.parameters import FieldSelector
from besos.problem import EPProblem
from eppy.modeleditor import IDF
import subprocess
import pandas as pd

global db

# deal with file path and for open folder in explorer
current_path = os.path.dirname(__file__)
FILEBROWSER_PATH = os.path.join(os.getenv('WINDIR'), 'explorer.exe')
def explore(path):
    path = os.path.normpath(path)

    if os.path.isdir(path):
        subprocess.run([FILEBROWSER_PATH, path])
    elif os.path.isfile(path):
        subprocess.run([FILEBROWSER_PATH, '/select,', os.path.normpath(path)])

# some useful default data
daily_data_standard_weekdays = [1, 1, 1, 1, 1, 1, 0.5, 0.5, 0.5, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2, 0.2, 0.5, 0.5, 0.5, 0.8, 0.8, 0.8, 1, 1]
daily_data_standard_weekends = [1, 1, 1, 1, 1, 1, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 0.8, 1, 1]
timeIntervalList = [
    '00:00-01:00',
    '01:00-02:00',
    '02:00-03:00',
    '03:00-04:00',
    '04:00-05:00',
    '05:00-06:00',
    '06:00-07:00',
    '07:00-08:00',
    '08:00-09:00',
    '09:00-10:00',
    '10:00-11:00',
    '11:00-12:00',
    '12:00-13:00',
    '13:00-14:00',
    '14:00-15:00',
    '15:00-16:00',
    '16:00-17:00',
    '17:00-18:00',
    '18:00-19:00',
    '19:00-20:00',
    '20:00-21:00',
    '21:00-22:00',
    '22:00-23:00',
    '23:00-24:00'
]
months_list = ['Jan', 'Feb', 'Mar', 'Apr','May','Jun', 'Jul', 'Aug','Sep', 'Oct', 'Nov', 'Dec']
x_ticks = [0, 31*24, 59*24, 90*24, 120*24, 151*24, 181*24, 212*24, 243*24, 273*24, 304*24, 334*24, 365*24-1]
x_label = ['Jan 01', 'Feb 01', 'Mar 01', 'Apr 01', 'May 01', 'Jun 01', 'Jul 01', 'Aug 01', 'Sep 01', 'Oct 01', 'Nov 01', 'Dec 01', 'Dec 31']
f_name_config = 'config.json'
# random.seed(59)

# some outputs for different language
output_dict = {
    'select_building': {'en':'You have selected the target building of ', 
                        'it':'Hai selezionato l\'edificio di destinazione di ', 
                        'ch':'您选择了目标建筑'},
    'results_not_available': {'en':'The results are not available yet.', 
                              'it':'I risultati non sono ancora disponibili.', 
                              'ch':'结果尚不可用。'},
    'software_title': {'en':'Smart Presence Desktop Software for Occupancy Profile', 
                       'it':'Software desktop Smart Presence per profilo di occupazione', 
                       'ch':'Smart Presence桌面软件'},
}

# generate schedule based on selected occupancy for target building or pesudo data
class OccuToSchedule():
    
    def __init__(self, occ_json_input, pattern, year='2022'):
        self.pattern = pattern
        self.year = year
        self.occupancy_dict = occ_json_input
        self.field_num = 1
        self.Hfield_num = 1
        self.Cfield_num = 1
        name = self.pattern +"_Occ"
        self.schedule = {"Name": name,"Schedule_Type_Limits_Name": "Fraction"}
        self.Hschedule = {"Name": "heating","Schedule_Type_Limits_Name": "Temperature"}
        self.Cschedule = {"Name": "cooling","Schedule_Type_Limits_Name": "Temperature"}
        self.months_list=['Jan', 'Feb', 'Mar', 'Apr','May','Jun', 'Jul', 'Aug','Sep', 'Oct', 'Nov', 'Dec']
        self.month_dict = {}
    
    # add schedule for occupancy    
    def __add_to_schedule(self,interval,occupancy):
        schedule_key = "Field_"+ str(self.field_num)
        schedule_time = int(interval.split(":")[0]) + 1
        if schedule_time > 9:
            schedule_interval = str(schedule_time) + ":00"
        else:
            schedule_interval = "0"+ str(schedule_time)+ ":00"
        schedule_value =  "Until: "+ schedule_interval
        self.schedule[schedule_key] = schedule_value
        self.field_num += 1
        schedule_key = "Field_"+ str(self.field_num)
        self.schedule[schedule_key] = occupancy
        self.field_num += 1
    
    # in order to coincide the occupancy with the heating and cooling, add schedule for heating and cooling    
    def __add_to_HCschedule(self,interval,occupancy,hc):
        if hc == 'H':
            schedule_key = "Field_"+ str(self.Hfield_num)
        else: 
            schedule_key = "Field_"+ str(self.Cfield_num)
        schedule_time = int(interval.split(":")[0]) + 1
        if schedule_time > 9:
            schedule_interval = str(schedule_time) + ":00"
        else:
            schedule_interval = "0"+ str(schedule_time)+ ":00"
        schedule_value =  "Until: "+ schedule_interval
        if hc == 'H':
            self.Hschedule[schedule_key] = schedule_value
            self.Hfield_num += 1
            schedule_key = "Field_"+ str(self.Hfield_num)
            self.Hschedule[schedule_key] = occupancy
            self.Hfield_num += 1
        else:
            self.Cschedule[schedule_key] = schedule_value
            self.Cfield_num += 1
            schedule_key = "Field_"+ str(self.Cfield_num)
            self.Cschedule[schedule_key] = occupancy
            self.Cfield_num += 1
    
    # create schedule for heating and cooling        
    def create_HCschedule(self, with_heating_cooling):
        interval_list = []
        occupancy_list = []
        for key,value in self.occupancy_dict.items():
            if key == "weekdaysAndWeekends_data":
                data_list = value
                for data in data_list:
                    interval_list.append(data.get("interval"))
                    occupancy_list.append(data.get("occupancy"))  
                    
        # Heating Schedule  
        keywords = []
        self.Hschedule['Field_'+ str(self.Hfield_num)] =  "Through: 31 Dec"
        self.Hfield_num += 1
        for interval in interval_list:
            interval_time = interval.split("-")[1] + ":00"
            keyword = interval.split("-")[0]
            if keyword not in keywords:
                if keyword == "weekdays":
                    self.Hschedule['Field_'+ str(self.Hfield_num)] =  "For: Weekdays WinterDesignDay"
                else:
                    self.Hschedule['Field_'+ str(self.Hfield_num)] = "For: Weekends"
                self.Hfield_num += 1
                keywords.append(keyword)
            occupancy = occupancy_list[interval_list.index(interval)]
            if with_heating_cooling:
                if occupancy >= 0.7:
                    occ_input = 1
                else:
                    occ_input = 0
            else:
                occ_input = occupancy
            self.__add_to_HCschedule(interval_time,occ_input,'H')
        self.Hschedule['Field_'+ str(self.Hfield_num)] = "For: SummerDesignDay AllOtherDays"
        self.Hfield_num += 1
        self.Hschedule['Field_'+ str(self.Hfield_num)] =  "Until: 24:00"
        self.Hfield_num += 1
        self.Hschedule['Field_'+ str(self.Hfield_num)] =  0
        
        # Cooling Schedule
        keywords = []
        self.Cschedule['Field_'+ str(self.Cfield_num)] =  "Through: 31 Dec"
        self.Cfield_num += 1
        for interval in interval_list:
            interval_time = interval.split("-")[1] + ":00"
            keyword = interval.split("-")[0]
            if keyword not in keywords:
                if keyword == "weekdays":
                    self.Cschedule['Field_'+ str(self.Cfield_num)] =  "For: Weekdays SummerDesignDay"
                else:
                    self.Cschedule['Field_'+ str(self.Cfield_num)] = "For: Weekends"
                self.Cfield_num += 1
                keywords.append(keyword)
            occupancy = occupancy_list[interval_list.index(interval)]
            if with_heating_cooling:
                if occupancy >= 0.7:
                    occ_input = 1
                else:
                    occ_input = 0
            else:
                occ_input = occupancy
            self.__add_to_HCschedule(interval_time,occ_input,'C')
        self.Cschedule['Field_'+ str(self.Cfield_num)] = "For: WinterDesignDay AllOtherDays"
        self.Cfield_num += 1
        self.Cschedule['Field_'+ str(self.Cfield_num)] =  "Until: 24:00"
        self.Cfield_num += 1
        self.Cschedule['Field_'+ str(self.Cfield_num)] =  0
            
        # save as json
        path = f"./output/{self.occupancy_dict['targetBuilding']}"
        isExist = os.path.exists(path)
        if not isExist:
            os.makedirs(path)
        with open(f"./output/{self.occupancy_dict['targetBuilding']}/schedule_Heating.json","w") as fp_h:
            json.dump(self.Hschedule, fp_h, indent=4)
        with open(f"./output/{self.occupancy_dict['targetBuilding']}/schedule_Cooling.json","w") as fp_c:
            json.dump(self.Cschedule, fp_c, indent=4)
    
    # create shcedule for occupancy    
    def create_schedule(
        self,
        end_time = '31 Dec', 
        pattern = '', 
        title = True, 
        year = '', 
        cuted_months = [], 
        months = ['Jan', 'Feb', 'Mar', 'Apr','May','Jun', 'Jul', 'Aug','Sep', 'Oct', 'Nov', 'Dec']
    ):
        if pattern == '':
            pattern = self.pattern
        if year == '':
            year = self.year
        interval_list = []
        occupancy_list = []
        for key,value in self.occupancy_dict.items():
            if key == pattern+"_data":
                data_list = value
                for data in data_list:
                    interval_list.append(data.get("interval"))
                    occupancy_list.append(data.get("occupancy"))
        if pattern == "daily":
            if title == True:
                self.schedule['Field_'+ str(self.field_num)] =  "Through: "+ end_time
                self.field_num += 1
                self.schedule['Field_'+ str(self.field_num)] = "For: AllDays"
                self.field_num += 1
            for interval in interval_list:
                interval_cut = interval[0:5]
                occupancy = occupancy_list[interval_list.index(interval)]
                self.__add_to_schedule(interval_cut,occupancy)
        elif pattern == "weekly":
            keywords = []
            days = {"Mon":"Monday","Tue":"Tuesday","Wed":"Wednesday","Thu":"Thursday","Fri":"Friday","Sat":"Saturday","Sun":"Sunday"}
            self.schedule['Field_'+ str(self.field_num)] =  "Through: "+ end_time
            self.field_num += 1
            for interval in interval_list:
                interval_time = interval.split("-")[2] + ":00"
                keyword = interval.split("-")[1]
                if keyword not in keywords:
                    day = days.get(keyword)
                    self.schedule['Field_'+ str(self.field_num)] = "For: " + day
                    self.field_num += 1
                    keywords.append(keyword)
                occupancy = occupancy_list[interval_list.index(interval)]
                self.__add_to_schedule(interval_time,occupancy)                           
        elif pattern == "weekdaysAndWeekends":
            keywords = []
            self.schedule['Field_'+ str(self.field_num)] =  "Through: "+ end_time
            self.field_num += 1
            for interval in interval_list:
                interval_time = interval.split("-")[1] + ":00"
                keyword = interval.split("-")[0]
                if keyword not in keywords:
                    if keyword == "weekdays":
                        self.schedule['Field_'+ str(self.field_num)] =  "For: Weekdays"
                    else:
                        self.schedule['Field_'+ str(self.field_num)] = "For: Weekends"
                    self.field_num += 1
                    keywords.append(keyword)
                occupancy = occupancy_list[interval_list.index(interval)]
                self.__add_to_schedule(interval_time,occupancy)
            self.schedule['Field_'+ str(self.field_num)] = "For: AllOtherDays"
            self.field_num += 1
            self.create_schedule(pattern='daily', title= False)
        elif pattern == "monthly":
            def getlist(date):
                return datetime.strptime(date, "%b-%H")
            interval_occ = zip(interval_list,occupancy_list)
            time_list = sorted(interval_occ,key = lambda date:getlist(date[0]))
            sorted_interval_list,sorted_occupancy_list = [list(x) for x in zip(*time_list)]
            lastday_list = []
            for month in self.months_list:
                monthnum = self.months_list.index(month)+1
                lastday = calendar.monthrange(int(self.year),monthnum)[1]
                lastday_list.append(lastday)
            self.month_dict = dict(zip(self.months_list,lastday_list))
            keywords = []
            done = []
            for interval in sorted_interval_list:
                interval_time = interval.split("-")[1] + ":00"
                keyword = interval.split("-")[0]
                if keyword in months:
                    if keyword not in keywords:
                        if keyword not in cuted_months:     
                            # generate a full month schedule
                            self.schedule['Field_'+ str(self.field_num)] =  "Through: "+ str(self.month_dict[keyword]) +' ' + keyword
                        else:
                            # invoke mix schedule and generate for corresponding date
                            self.schedule['Field_'+ str(self.field_num)] =  "Through: "+ end_time
                        self.field_num += 1
                        self.schedule['Field_'+ str(self.field_num)] = "For: AllDays"
                        self.field_num += 1
                        keywords.append(keyword) 
                    occupancy = sorted_occupancy_list[sorted_interval_list.index(interval)]
                    if keyword not in done and occupancy == -1:
                        self.create_schedule(pattern='daily', title= False)
                        done.append(keyword)
                        continue
                    elif occupancy == -1:
                        continue
                    self.__add_to_schedule(interval_time,occupancy)
        elif pattern == "yearly":
            keywords = []
            self.schedule['Field_'+ str(self.field_num)] =  "Through: "+ end_time
            self.field_num += 1
            for interval in interval_list:
                interval_time = interval.split("-")[1] + ":00"
                keyword = interval.split("-")[0]
                if keyword not in keywords:
                    if keyword == year:
                        self.schedule['Field_'+ str(self.field_num)] =  "For: AllDays"
                    else:
                        continue
                    self.field_num += 1
                    keywords.append(keyword)
                occupancy = occupancy_list[interval_list.index(interval)]
                self.__add_to_schedule(interval_time,occupancy)
        
        # save as json
        path = f"./output/{self.occupancy_dict['targetBuilding']}"
        isExist = os.path.exists(path)
        if not isExist:
            os.makedirs(path)
        with open(f"./output/{self.occupancy_dict['targetBuilding']}/schedule.json","w") as fp:
            json.dump(self.schedule, fp, indent=4)
            
    def mix_schedule(self,inputs=[]):
        self.schedule = {"Name": 'Mix_Occ',"Schedule_Type_Limits_Name": "Fraction"}
        year = self.year
        for input in inputs:
            input[0] = datetime.strptime(input[0], "%d %b")
        sorted_inputs = sorted(inputs,key=lambda k:k[0])
        if self.pattern == "monthly":
            months_list=['Jan', 'Feb', 'Mar', 'Apr','May','Jun', 'Jul', 'Aug','Sep', 'Oct', 'Nov', 'Dec']
            cuted_months = []
            months = []
            for input in sorted_inputs:
                input[0] = datetime.strftime(input[0], "%d %b")
                start_date = input[0]
                end_date = input[1]
                mix_pattern = input[2]
                try:
                    year = input[3]
                except:
                    pass
                month_start = start_date.split(' ')[1]
                month_end = end_date.split(' ')[1]
                if mix_pattern != self.pattern:
                    if start_date == '01 Jan':
                        self.create_schedule(end_date,mix_pattern,year=year)
                        for m in months_list:
                           if m != month_end and m not in months:
                               months.append(m)
                           elif m == month_end and m not in months:
                                monthnum = months_list.index(m)+1
                                lastday = calendar.monthrange(int(year),monthnum)[1]
                                if int(end_date.split(' ')[0]) != lastday:
                                    self.create_schedule(end_date,months=[m])
                                months.append(m)
                                break
                    else:
                        cuted_months.append(month_start)
                        #cuted_months.append(month_end)
                        for m in months_list:
                            if m != month_start and m not in months:
                                months.append(m)
                                self.create_schedule(start_date,months=[m])
                            elif m == month_start and m not in months:
                                months.append(m)
                                self.create_schedule(start_date,months=[m], cuted_months=cuted_months)
                                break
                        self.create_schedule(end_date,mix_pattern,year=year)
                    for m in months_list:
                        monthnum = months_list.index(m)+1
                        lastday = calendar.monthrange(int(year),monthnum)[1]
                        if m not in months:
                            if m != month_end:
                                months.append(m)
                            elif int(end_date.split(' ')[0]) == lastday:
                                months.append(m)
                                break
                            else:
                                break  
                elif start_date == '01 Jan' and end_date == '31 Dec':
                    self.create_schedule(mix_pattern)
                    break
                elif end_date == '31 Dec':
                    for m in months_list:
                        if m not in months:
                            self.create_schedule(end_date,months=[m])
                            months.append(m)
                else:
                    continue
            if end_date != '31 Dec':
                for m in months_list:
                    if m not in months:
                        self.create_schedule(end_date,months=[m])
                        months.append(m)
        else: # if self.pattern != 'monthly'
            months_list=['Jan', 'Feb', 'Mar', 'Apr','May','Jun', 'Jul', 'Aug','Sep', 'Oct', 'Nov', 'Dec']
            cuted_months = []
            months = []
            for input in sorted_inputs:
                input[0] = datetime.strftime(input[0], "%d %b")
                start_date = input[0]
                end_date = input[1]
                mix_pattern = input[2]
                try:
                    year = input[3]
                except:
                    pass
                month_start = start_date.split(' ')[1]
                month_end = end_date.split(' ')[1]
                if mix_pattern != 'monthly':
                    if mix_pattern != self.pattern:
                        if start_date == '01 Jan':
                            self.create_schedule(end_date,mix_pattern,year=year)
                        else:
                            self.create_schedule(start_date,year=year)
                            self.create_schedule(end_date,mix_pattern,year=year)
                    elif start_date == '01 Jan' and end_date == '31 Dec':
                        self.create_schedule(end_date,mix_pattern,year=year)
                        break
                    else:
                        continue
                else: # if mix_pattern == 'monthly'
                    if start_date == '01 Jan' and end_date == '31 Dec':
                        self.create_schedule(end_date,mix_pattern,year=year)
                        break
                    elif start_date == '01 Jan':
                        for m in months_list:
                           if m != month_end and m not in months:
                               self.create_schedule(end_date,mix_pattern,months=[m])
                               months.append(m)
                           elif m == month_end and m not in months:
                                monthnum = months_list.index(m)+1
                                lastday = calendar.monthrange(int(year),monthnum)[1]
                                if int(end_date.split(' ')[0]) != lastday:
                                    cuted_months.append(m)
                                self.create_schedule(end_date,mix_pattern,months=[m],cuted_months=cuted_months)
                                months.append(m)
                                break
                    else:
                        self.create_schedule(start_date,year=year)
                        for m in months_list:
                            if m not in months:
                                if months_list.index(m) == months_list.index(month_start):
                                    monthnum = months_list.index(m)+1
                                    lastday = calendar.monthrange(int(year),monthnum)[1]
                                    if int(start_date.split(' ')[0]) != lastday:
                                        self.create_schedule(pattern= mix_pattern, months=[m])
                                elif months_list.index(m) > months_list.index(month_start) and m != month_end:
                                    self.create_schedule(pattern= mix_pattern,months=[m])
                                elif m == month_end:
                                    monthnum = months_list.index(m)+1
                                    lastday = calendar.monthrange(int(year),monthnum)[1]
                                    if int(end_date.split(' ')[0]) != lastday:
                                        cuted_months.append(m)
                                    self.create_schedule(end_date,pattern= mix_pattern,months=[m], cuted_months=cuted_months)
                                    
                                    break
                                
            if end_date != '31 Dec':
                self.create_schedule('31 Dec',year=year)
                
# perform simulation locally with schedule based on selected occupancy and standard
class Simulation():
    def __init__(self, idf_path, object_name, epw_path, input_name, with_heating_cooling, with_standard=True):
        self.object_name = object_name
        self.building = ef.get_building(idf_path)
        self.building_standard = ef.get_building(idf_path)
        self.idf_file = idf_path
        self.epw_file = epw_path
        self.standard = json.loads(open("./config/schedule_standard.json", 'r').read())
        if with_heating_cooling:
            self.Hstandard = json.loads(open("./config/schedule_standard_H_on_off.json", 'r').read())
            self.Cstandard = json.loads(open("./config/schedule_standard_C_on_off.json", 'r').read())
        else:
            self.Hstandard = json.loads(open("./config/schedule_standard_H.json", 'r').read())
            self.Cstandard = json.loads(open("./config/schedule_standard_C.json", 'r').read())
        self.schedule_dict = json.loads(open(f"./output/{input_name}/schedule.json", 'r').read())
        self.Hschedule_dict = json.loads(open(f"./output/{input_name}/schedule_Heating.json", 'r').read())
        self.Cschedule_dict = json.loads(open(f"./output/{input_name}/schedule_Cooling.json", 'r').read())
        self.input_name = input_name
        self.with_standard = with_standard

    # modify idf file with standard occupancy and generate new idf file
    def modify_standardIDF(self):
        for key,value in self.standard.items():
            if key != "Name" and key != "Schedule_Type_Limits_Name":
                schedule = FieldSelector(   class_name = 'Schedule:Compact',\
                                            object_name = self.object_name,\
                                            field_name = key)
                schedule.set(self.building_standard, value)
        for key,value in self.Hstandard.items():
            if key != "Name" and key != "Schedule_Type_Limits_Name":
                schedule = FieldSelector(   class_name = 'Schedule:Compact',\
                                            object_name = 'Block1:Zone1 Heating Availability Sch',\
                                            field_name = key)
                schedule.set(self.building_standard, value)
        for key,value in self.Cstandard.items():
            if key != "Name" and key != "Schedule_Type_Limits_Name":
                schedule = FieldSelector(   class_name = 'Schedule:Compact',\
                                            object_name = 'Block1:Zone1 Cooling Availability Sch',\
                                            field_name = key)
                schedule.set(self.building_standard, value)
        self.building_standard.saveas(f"./output/{self.input_name}/standardIDF.idf")
    
    # modify idf file with selected occupancy and generate new idf file
    def modify_IDF(self):
        for key,value in self.schedule_dict.items():
            if key != "Name" and key != "Schedule_Type_Limits_Name":
                schedule = FieldSelector(   class_name = 'Schedule:Compact',\
                                            object_name = self.object_name,\
                                            field_name = key)
                schedule.set(self.building, value)
        for key,value in self.Hschedule_dict.items():
            if key != "Name" and key != "Schedule_Type_Limits_Name":
                schedule = FieldSelector(   class_name = 'Schedule:Compact',\
                                            object_name = 'Block1:Zone1 Heating Availability Sch',\
                                            field_name = key)
                schedule.set(self.building, value)
        for key,value in self.Cschedule_dict.items():
            if key != "Name" and key != "Schedule_Type_Limits_Name":
                schedule = FieldSelector(   class_name = 'Schedule:Compact',\
                                            object_name = 'Block1:Zone1 Cooling Availability Sch',\
                                            field_name = key)
                schedule.set(self.building, value)
        self.building.saveas(f"./output/{self.input_name}/modifiedIDF.idf")

    # use eepy to perform simulation and generate outputs
    def run_eppy(self):
        iddfile='C:/EnergyPlusV9-4-0/Energy+.idd'
        IDF.setiddname(iddfile)
        standard_idf = IDF(f"./output/{self.input_name}/standardIDF.idf", self.epw_file)
        idf = IDF(f"./output/{self.input_name}/modifiedIDF.idf", self.epw_file)
        output_dir = f"./output/{self.input_name}/eppy_outputs"
        output_dir_standard = f"./output/{self.input_name}/eppy_outputs_standard"
        if self.with_standard:
            try:
                standard_idf.run(expandobjects=True, output_directory=output_dir_standard, readvars=True, output_suffix="L")
            except:
                pass
        idf.run(expandobjects=True, output_directory=output_dir, readvars=True, output_suffix="L")   

# calculate occupancy for pseudo data for different time scale (the similar procedure is also in data processing for real collected data)
class occupancycalculate():
    def __init__(self,rawoccupancy):
        self.rawoccupancy = rawoccupancy

    def daily_occupancy(self):
        self.daily_dict={}
        self.daily_count={}
        
        for i in range(0,24):
            j= str("%02d"%i)+":00"
            self.daily_dict.update({j:0})
            self.daily_count.update({j:0})
            
        for key in self.rawoccupancy:
            if self.rawoccupancy[key]==-1:
                continue
            self.daily_dict[key[11:13]+":00"]=self.daily_dict[key[11:13]+":00"]+self.rawoccupancy[key]
            self.daily_count[key[11:13]+":00"]=self.daily_count[key[11:13]+":00"]+1
        for key in  self.daily_dict:
            if self.daily_count[key]==0:
                self.daily_dict[key]=-1
                continue
            self.daily_dict[key]=self.daily_dict[key]/self.daily_count[key]
        return self.daily_dict
  
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
            self.weekdayoftheday = datetime.strptime(key[0:10], "%Y-%m-%d").weekday()
            
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
            self.weekdayoftheday = datetime.strptime(key[0:10], "%Y-%m-%d").weekday()
            
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

# generate pseudo data based on standard for different scenarios
class pseudoDataGeneration():
    
    def __init__(self,start_time,end_time,time_interval=1):
        self.mean = 0 
        self.variance = 0
        self.pure_standard = {}
        self.pseudo_occupant = {}
        self.time_interval = time_interval
        self.start_time = datetime.strptime(start_time,"%Y-%m-%d-%H")
        self.end_time = datetime.strptime(end_time,"%Y-%m-%d-%H")
        # if self.start_time > self.end_time:
        #     raise SystemExit("End time must be larger than start time!")
        try:
            with open(f'{current_path}/config/standard_weekdays.json', 'r') as f:
                self.standard_weekdays=json.load(f)
            with open(f'{current_path}/config/standard_weekends.json', 'r') as f:
                self.standard_weekends=json.load(f)
        except:
            self.standard_weekdays = {
                "2000-01-01-00": 1,
                "2000-01-01-01": 1,
                "2000-01-01-02": 1,
                "2000-01-01-03": 1,
                "2000-01-01-04": 1,
                "2000-01-01-05": 1,
                "2000-01-01-06": 0.5,
                "2000-01-01-07": 0.5,
                "2000-01-01-08": 0.5,
                "2000-01-01-09": 0.1,
                "2000-01-01-10": 0.1,
                "2000-01-01-11": 0.1,
                "2000-01-01-12": 0.1,
                "2000-01-01-13": 0.2,
                "2000-01-01-14": 0.2,
                "2000-01-01-15": 0.2,
                "2000-01-01-16": 0.5,
                "2000-01-01-17": 0.5,
                "2000-01-01-18": 0.5,
                "2000-01-01-19": 0.8,
                "2000-01-01-20": 0.8,
                "2000-01-01-21": 0.8,
                "2000-01-01-22": 1,
                "2000-01-01-23": 1
            }
            self.standard_weekends = {
                "2000-01-01-00": 1,
                "2000-01-01-01": 1,
                "2000-01-01-02": 1,
                "2000-01-01-03": 1,
                "2000-01-01-04": 1,
                "2000-01-01-05": 1,
                "2000-01-01-06": 0.8,
                "2000-01-01-07": 0.8,
                "2000-01-01-08": 0.8,
                "2000-01-01-09": 0.8,
                "2000-01-01-10": 0.8,
                "2000-01-01-11": 0.8,
                "2000-01-01-12": 0.8,
                "2000-01-01-13": 0.8,
                "2000-01-01-14": 0.8,
                "2000-01-01-15": 0.8,
                "2000-01-01-16": 0.8,
                "2000-01-01-17": 0.8,
                "2000-01-01-18": 0.8,
                "2000-01-01-19": 0.8,
                "2000-01-01-20": 0.8,
                "2000-01-01-21": 0.8,
                "2000-01-01-22": 1,
                "2000-01-01-23": 1
            }

    def Gene_Pesudo(self,scenarios=0,cus_mean=0,cus_variance=0):    #Generate pseudo data for the given time peroid + options of noise
        time = self.start_time
        while time <= self.end_time:
            if time.isoweekday() == 6 or time.isoweekday() == 7:
                isweekday = False
                for key in self.standard_weekends.keys():
                    key = datetime.strptime(key,"%Y-%m-%d-%H")
                    if datetime.time(key) == datetime.time(time):
                        self.mean = self.standard_weekends[datetime.strftime(key,"%Y-%m-%d-%H")]
                        self.pure_standard[datetime.strftime(time,"%Y-%m-%d-%H")] = self.mean
            else:
                isweekday = True
                for key in self.standard_weekdays.keys():
                    key = datetime.strptime(key,"%Y-%m-%d-%H")
                    if datetime.time(key) == datetime.time(time):
                        self.mean = self.standard_weekdays[datetime.strftime(key,"%Y-%m-%d-%H")]
                        self.pure_standard[datetime.strftime(time,"%Y-%m-%d-%H")] = self.mean
            self.__conf_noise(scenarios,isweekday,time,cus_mean,cus_variance)
            time = time + timedelta(hours=self.time_interval)
        return self.__pesudo_Occupancy()

    # customized noise for different scenarios
    def __conf_noise(self,scenarios,isweekday,time,cus_mean,cus_variance):     #configurable noise and multiple scenarios provided
        self.scenarios = scenarios
        while True:
            if self.scenarios == 0:
                self.pseudo_occupant = self.pure_standard
                break
            elif self.scenarios == 1:
                if isweekday == False:
                    cus_mean = self.mean
                    cus_variance = 0.2
                elif isweekday == True:
                    cus_mean = self.mean
                    cus_variance = 0.1
            elif self.scenarios == 2:
                if isweekday == False:
                    cus_mean = self.mean-0.05
                    cus_variance = 0.2
                elif isweekday == True:
                    cus_mean = self.mean+0.05
                    cus_variance = 0.1 
            elif self.scenarios == 3:   
                if isweekday == False:
                    cus_mean = self.mean+0.05
                    cus_variance = 0.2
                elif isweekday == True:
                    cus_mean = self.mean+0.05
                    cus_variance = 0.1 
            elif self.scenarios == 4:
                cus_mean = self.mean-1
                cus_variance = 0.2
            elif self.scenarios == 5:
                cus_mean = self.mean+0.8
                cus_variance = 0.05
            elif self.scenarios == 6:
                cus_mean = self.mean
            pseudo_data = random.gauss(cus_mean,cus_variance)
            pseudo_data = int(pseudo_data*1000)/1000
            if time.hour <= 4 and self.scenarios != 4:
                self.pseudo_occupant[datetime.strftime(time,"%Y-%m-%d-%H")] = 1.0
            elif time.hour <= 6 and self.scenarios == 4:
                self.pseudo_occupant[datetime.strftime(time,"%Y-%m-%d-%H")] = 1.0
            else:
                if pseudo_data <= 0:
                    self.pseudo_occupant[datetime.strftime(time,"%Y-%m-%d-%H")] = 0.0
                elif pseudo_data > 1:
                    self.pseudo_occupant[datetime.strftime(time,"%Y-%m-%d-%H")] = 1.0
                else:
                    self.pseudo_occupant[datetime.strftime(time,"%Y-%m-%d-%H")] = pseudo_data
            break

    # output occupancy based on daily 24h data
    def __pesudo_Occupancy(self):
        resultsHourlyOccupancy = occupancycalculate(self.pseudo_occupant)
        dailyData = resultsHourlyOccupancy.daily_occupancy()
        weeklyData = resultsHourlyOccupancy.weekly_occupancy()
        monthlyData = resultsHourlyOccupancy.monthly_occupancy()
        yearlyData = resultsHourlyOccupancy.yearly_occupancy()
        weekdayAndWeekendData = resultsHourlyOccupancy.workdaysandweekends_occupancy()
        resultOccupancy = {}
        resultOccupancy['results_availability'] = {
            "weekly_available": True,
            "monthly_available": False,
            "daily_available": True,
            "total_available": True,
            "yearly_available": True,
            "monthly_12_available": True
        }
        resultOccupancy['total_data'] = [{'interval': key, 'occupancy': self.pseudo_occupant[key]} for key in self.pseudo_occupant.keys()]
        resultOccupancy['weekly_data'] = [{'interval': key, 'occupancy': weeklyData[key]} for key in weeklyData.keys()]
        resultOccupancy['daily_data'] = [{'interval': key, 'occupancy': dailyData[key]} for key in dailyData.keys()]
        resultOccupancy['monthly_data'] = [{'interval': key, 'occupancy': monthlyData[key]} for key in monthlyData.keys()]
        resultOccupancy['yearly_data'] = [{'interval': key, 'occupancy': yearlyData[key]} for key in yearlyData.keys()]
        resultOccupancy['weekdaysAndWeekends_data'] = [{'interval': key, 'occupancy': weekdayAndWeekendData[key]} for key in weekdayAndWeekendData.keys()]
        # resultOccupancy['total_data'] = self.pseudo_occupant
        # resultOccupancy['weekly_data'] = weeklyData
        # resultOccupancy['daily_data'] = dailyData
        # resultOccupancy['monthly_data'] = monthlyData
        # resultOccupancy['yearly_data'] = yearlyData
        
        return resultOccupancy

# download all occupancy results for selected target building from firebase firestore database
def getAllResultsForTargetBuilding(target, windowOutput, lang):
    doc_ref = db.collection(target).document(u'Results')
    doc = doc_ref.get()
    if doc.exists:
        # windowOutput.update(f'You have selected the target building of {target}')
        windowOutput.update(output_dict['select_building'][lang] + target + '.')
        data = {}
        data['results_availability'] = doc.to_dict()
        collections = db.collection(target).document('Results').collections()
        for collection in collections:
            tempDocs = []
            for doc in collection.stream():
                tempDocs.append(doc.to_dict())
            data[collection.id] = tempDocs.copy()
        return data
    else:
        # windowOutput.update('The results are not available yet.')
        windowOutput.update(output_dict['results_not_available'][lang])
        return {}

# for all the following plots except the one for all data, comparisons with standards are available
# output plot for daily occupancy    
def drawDailyPlot(x_time_label, y_occupancy, withStandardOrNot, target):
    figure = matplotlib.figure.Figure(figsize=(5, 5), dpi=100, tight_layout=True)
    figure.add_subplot(111, title='Daily Occupancy Profile', xlabel='Time interval', ylabel='Occupancy[-]', ylim=(0.0, 1.1)).plot(x_time_label, y_occupancy)
    figure.gca().plot(x_time_label, y_occupancy, 'o', color='black', label=target)
    figure.gca().fill_between(x_time_label, y_occupancy, color='red')
    if withStandardOrNot:
        figure.gca().plot(x_time_label, daily_data_standard_weekdays)
        figure.gca().plot(x_time_label, daily_data_standard_weekdays, 'o', color='blue', label='EN 16798-1:2019')
        figure.gca().fill_between(x_time_label, daily_data_standard_weekdays, color='green')
    figure.legend(fontsize='xx-small')
    figure.gca().set_xticklabels(labels=x_time_label, rotation=90)
    
    return figure

# output plot for weekly occupancy (from Mon to Sun and weekdays as well as weekends)    
def drawWeeklyPlot(x_all, y_all, x_weekday, y_weekday, x_weekend, y_weekend, withStandardOrNot, target):       
    fig = plt.figure(figsize=(15, 8))
    plt.axis('off')
    gs = fig.add_gridspec(2, 2)
    
    ax1 = fig.add_subplot(gs[0, :])
    ax1.set_xticklabels(labels=x_all, rotation=90, fontsize=6)
    ax1.set_ylim([0, 1.1])
    ax1.plot(x_all, y_all)
    ax1.plot(x_all, y_all, 'o', color='black', label=target)
    ax1.set_xticklabels(labels=x_all, rotation=90, fontsize=6)
    ax1.set_xlabel('Time interval', fontsize=6)
    ax1.set_ylabel('Occupancy[-]')
    ax1.set_title('Weekly Occupancy Profile')
    ax1.fill_between(x_all, y_all, color='red')
    for i in range(1,7):
        ax1.axvline(24*i, ymin=0, ymax=1/1.1, color='b')
    if (withStandardOrNot):
        tempStandardData = []
        for i in range(0,5):
            tempStandardData.extend(daily_data_standard_weekdays)
        tempStandardData.extend(daily_data_standard_weekends)
        tempStandardData.extend(daily_data_standard_weekends)
        ax1.plot(x_all, tempStandardData)
        ax1.plot(x_all, tempStandardData, 'o', color='blue', label='EN 16798-1:2019')
        ax1.fill_between(x_all, tempStandardData, color='green')
        
    ax2 = fig.add_subplot(gs[1, 0])
    ax2.set_xticklabels(labels=x_weekday, rotation=90, fontsize=6)
    ax2.set_ylim([0, 1.1])
    ax2.plot(x_weekday, y_weekday)
    ax2.plot(x_weekday, y_weekday, 'o', color='black', label=target)
    ax2.set_xticklabels(labels=x_weekday, rotation=90, fontsize=6)
    ax2.set_xlabel('Time interval', fontsize=6)
    ax2.set_ylabel('Occupancy[-]')
    ax2.set_title('Weekday Occupancy Profile')
    ax2.fill_between(x_weekday, y_weekday, color='red')
    if (withStandardOrNot):
        ax2.plot(x_weekday, daily_data_standard_weekdays)
        ax2.plot(x_weekday, daily_data_standard_weekdays, 'o', color='blue', label='EN 16798-1:2019')
        ax2.fill_between(x_weekday, daily_data_standard_weekdays, color='green')
        
    ax3 = fig.add_subplot(gs[1, 1])  
    ax3.set_xticklabels(labels=x_weekend, rotation=90, fontsize=6)
    ax3.set_ylim([0, 1.1])
    ax3.plot(x_weekend, y_weekend)
    ax3.plot(x_weekend, y_weekend, 'o', color='black', label=target)
    ax3.set_xticklabels(labels=x_weekend, rotation=90, fontsize=6)
    ax3.set_xlabel('Time interval', fontsize=6)
    ax3.set_ylabel('Occupancy[-]')
    ax3.set_title('Weekend Occupancy Profile')
    ax3.fill_between(x_weekend, y_weekend, color='red')
    if (withStandardOrNot):
        ax3.plot(x_weekend, daily_data_standard_weekends)
        ax3.plot(x_weekend, daily_data_standard_weekends, 'o', color='blue', label='EN 16798-1:2019')
        ax3.fill_between(x_weekend, daily_data_standard_weekends, color='green')
    
    plt.legend()
    plt.tight_layout()
    plt.show(block=False)
         
# output plot for monthly occupancy (from Jan to Dec if they are available)
def drawMonthlyPlot(occupancyValues, withStandardOrNot, target):
    fig, axs = plt.subplots(3, 4, sharex=True, sharey=True, figsize=(15, 7))
    fig.suptitle('Daily occupancy for each month')
    row = 0
    col = 0
    for key in occupancyValues.keys():
        if occupancyValues[key][0] == -1:
            axs[row,col].text(11, 0.5, 'The results are not available', ha='center', va='center', fontsize=9)
        else:
            axs[row,col].plot(timeIntervalList, occupancyValues[key])
            axs[row,col].plot(timeIntervalList, occupancyValues[key], 'o', color='black', label=target)
            axs[row,col].fill_between(timeIntervalList, occupancyValues[key], color='red')
            if withStandardOrNot:
                axs[row,col].plot(timeIntervalList, daily_data_standard_weekdays)
                axs[row,col].plot(timeIntervalList, daily_data_standard_weekdays, 'o', color='blue', label='EN 16798-1:2019')
                axs[row,col].fill_between(timeIntervalList, daily_data_standard_weekdays, color='green')
        axs[row,col].set_xticklabels(labels=timeIntervalList, rotation=90, fontsize=5)
        axs[row,col].set_title(key)
        axs[row,col].set_ylabel('Occupancy[-]')
        axs[row,col].set_ylim([0, 1.1])
        axs[row,col].label_outer()
        col += 1
        if col == 4:
            col = 0
            row += 1
    plt.legend(loc='center left', fontsize='xx-small', bbox_to_anchor=(1, 0.5))
    plt.show(block=False)
        
# output plot for yearly occupancy (you can select certain year)
def drawYearlyPlot(x_time_label, y_occupancy, year, withStandardOrNot, target):
    figure = matplotlib.figure.Figure(figsize=(5, 5), dpi=100, tight_layout=True)
    figure.add_subplot(111, title=f'Daily Occupancy Profile in {year}', xlabel='Time interval', ylabel='Occupancy[-]', ylim=(0.0, 1.1)).plot(x_time_label, y_occupancy)
    figure.gca().plot(x_time_label, y_occupancy, 'o', color='black', label=target)
    figure.gca().fill_between(x_time_label, y_occupancy, color='red')
    if withStandardOrNot:
        figure.gca().plot(x_time_label, daily_data_standard_weekdays)
        figure.gca().plot(x_time_label, daily_data_standard_weekdays, 'o', color='blue', label='EN 16798-1:2019')
        figure.gca().fill_between(x_time_label, daily_data_standard_weekdays, color='green')
    figure.legend(fontsize='xx-small')
    figure.gca().set_xticklabels(labels=x_time_label, rotation=90)
    
    return figure

# the follwing two functions are for generate a block for year selection
def block_focus(window):
    for key in window.key_dict:    
        element = window[key]
        if isinstance(element, sg.Button):
            element.block_focus()
    
def popupYearSelection(btnList):
    layout = [
        [sg.Text('Select the year')],
        btnList
    ]
    window = sg.Window("Year selection", layout, use_default_focus=False, finalize=True, modal=True)
    block_focus(window)
    event, values = window.read()
    window.close()
    
    return event

# output plot for all data, from the first day for starting the collection until the last day            
def drawAllPlot(x_time_label, y_occupancy, target):       
    plt.rcParams["figure.figsize"] = (15,5)
    plt.title('Hourly Occupancy through All Collected Data')
    if target[0:6] == 'pesudo':
        plt.axes().set_ylim([0, 1.1])
    else:
        plt.axes().set_ylim([-0.2, 1.1])
    plt.plot(x_time_label, y_occupancy, label=target)
    plt.axes().set_xticks([0, len(x_time_label)-1])
    plt.axes().set_xticks([], minor=True)
    plt.axes().set_xticklabels([x_time_label[0], x_time_label[-1]])
    plt.axes().set_xlabel('Time instance', fontsize=10)
    plt.axes().set_ylabel('Occupancy[-]')
    plt.axes().fill_between(x_time_label, y_occupancy, color='red')
    plt.legend()
    plt.tight_layout()
    plt.show(block=False)

# through this way, the plot can be shown inside the GUI
def draw_figure(canvas, figure):
    figure_canvas_agg = FigureCanvasTkAgg(figure, canvas)
    figure_canvas_agg.draw()
    figure_canvas_agg.get_tk_widget().pack(side="top", fill="both", expand=1)
    return figure_canvas_agg

# store language configuration 
def change_configuration(lang, help_text):
    with open(f'{current_path}/config/{f_name_config}', 'w') as f:
        content = {'language': lang, 'help_text': help_text}
        json.dump(content, f)

# generate access code for access all data in mobile app        
def checkEmailAndOutput(email):
    doc_ref = db.collection(u'RegisteredUser').document(email)
    doc = doc_ref.get()
    if doc.exists:
        content = doc.to_dict()
        if 'accessCode' in content.keys():
            return 'You already have permission granted, the code is: ' + content['accessCode']
        else:
            code = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
            data = {u'accessCode': code}
            doc_ref.set(data, merge=True)
            return code
    else:
        return 'Your email is invalid or incorrect'

# the following three functions are for generating checking and modifying schedule combination of different time scale     
def generateDateDict():
    sdate = date(2022,1,1)  
    edate = date(2023,1,1)   
    date_list = [(sdate+timedelta(days=x)).strftime("%m-%d") for x in range((edate-sdate).days)]
    output = {}
    for d in date_list:
        output[d] = True
        
    return output

def checkAlreadySelectedDate(dict_input, start_input, stop_input):
    sdate = date(2022,int(start_input[0:2]),int(start_input[3:5]))  
    edate = date(2022,int(stop_input[0:2]),int(stop_input[3:5])) + timedelta(days=1)
    date_list = [(sdate+timedelta(days=x)).strftime("%m-%d") for x in range((edate-sdate).days)]
    output = True
    for d in date_list:
        if not dict_input[d]:
            output = False
            break
    
    return output

def changeDateDict(dict_input, start_input, stop_input):
    sdate = date(2022, int(start_input[0:2]), int(start_input[3:5]))  
    edate = date(2022, int(stop_input[0:2]), int(stop_input[3:5])) + timedelta(days=1)
    date_list = [(sdate+timedelta(days=x)).strftime("%m-%d") for x in range((edate-sdate).days)]
    for d in date_list:
        dict_input[d] = False

# function for drawing the total annual energy consumption for simulation result from both my schedule and standard        
def draw_total_consumption(df_o, df_s, name):
    figure = matplotlib.figure.Figure(figsize=(5.5, 5), dpi=100)
    figure.add_subplot(111, title='Total annual energy consumption', ylabel='Consumption[kWh]')
    barWidth = 0.25
    try:
        heat_o = df_o['DistrictHeating:Facility [J](Hourly)'].sum() / 3.6e6
        heat_s = df_s['DistrictHeating:Facility [J](Hourly)'].sum() / 3.6e6
        cool_o = df_o['DistrictCooling:Facility [J](Hourly)'].sum() / 3.6e6
        cool_s = df_s['DistrictCooling:Facility [J](Hourly)'].sum() / 3.6e6
        ele_o = df_o['Electricity:Facility [J](Hourly)'].sum() / 3.6e6
        ele_s = df_s['Electricity:Facility [J](Hourly)'].sum() / 3.6e6
        list_o = [heat_o, cool_o, ele_o]
        list_s = [heat_s, cool_s, ele_s]
        br1 = [0, barWidth*3, barWidth*6]
        br2 = [barWidth, barWidth*4, barWidth*7]
        bar1 = figure.gca().bar(br1, list_o, color ='b', width = barWidth, edgecolor ='grey', label = name)
        bar2 = figure.gca().bar(br2, list_s, color ='g', width = barWidth, edgecolor ='grey', label = 'standard')
        i = 0
        for rect in bar1:
            height = rect.get_height()
            figure.gca().text(rect.get_x() + rect.get_width() / 2.0, height, format(list_o[i], '.1f'), ha='center', va='bottom')
            i += 1
        i = 0
        for rect in bar2:
            height = rect.get_height()
            figure.gca().text(rect.get_x() + rect.get_width() / 2.0, height, format(list_s[i], '.1f'), ha='center', va='bottom')
            i += 1
        figure.legend(fontsize='xx-small')
        figure.gca().set_xticks([barWidth/2, barWidth*3+barWidth/2, barWidth*6+barWidth/2])
        figure.gca().set_xticklabels(['Heating', 'Cooling', 'Electricity'])
        
        return figure
    except:
        heat_o = df_o['DistrictHeating:Facility [J](Hourly)'].sum() / 3.6e6
        cool_o = df_o['DistrictCooling:Facility [J](Hourly)'].sum() / 3.6e6
        ele_o = df_o['Electricity:Facility [J](Hourly)'].sum() / 3.6e6
        list_o = [heat_o, cool_o, ele_o]
        br = [0, barWidth*2, barWidth*4]
        bar = figure.gca().bar(br, list_o, color ='b', width = barWidth, edgecolor ='grey', label = name)
        i = 0
        for rect in bar:
            height = rect.get_height()
            figure.gca().text(rect.get_x() + rect.get_width() / 2.0, height, format(list_o[i], '.1f'), ha='center', va='bottom')
            i += 1
        figure.legend(fontsize='xx-small')
        figure.gca().set_xticks(br)
        figure.gca().set_xticklabels(['Heating', 'Cooling', 'Electricity'])
        
        return figure
    
# function for drawing the annual temperature for simulation result
def draw_temperature(df_o, df_s, name):
    try:
        fig, (ax1, ax2) = plt.subplots(2, 1, sharex=True, figsize=(15, 8))
        fig.suptitle('Annual hourly indoor temeprature')
        temp_o = df_o['BLOCK1:ZONE1:Zone Mean Air Temperature [C](Hourly:ON)']
        temp_s = df_s['BLOCK1:ZONE1:Zone Mean Air Temperature [C](Hourly:ON)']
        ax1.plot(temp_o, color='g', label=name)
        ax2.plot(temp_s, label='standard')
        ax1.set_xticks(x_ticks)
        ax1.set_ylabel('Temperature[degree C]')
        ax1.set_xticklabels(x_label)
        plt.xlabel('Date')
        plt.ylabel('Temperature[degree C]')
        ax1.legend()
        ax2.legend()
        ax1.grid()
        ax2.grid()
        plt.show(block=False)
    except:
        temp_o = df_o['BLOCK1:ZONE1:Zone Mean Air Temperature [C](Hourly:ON)']
        plt.figure(figsize=(15, 8))
        plt.plot(temp_o, label=name)
        plt.title('Annual hourly indoor temeprature')
        plt.xticks(x_ticks, x_label)
        plt.xlabel('Date')
        plt.ylabel('Temperature[degree C]')
        plt.legend()
        plt.grid()
        plt.tight_layout()
        plt.show(block=False)
        
# function for drawing the annual heating, cooling and electricity consumption for simulation result
def draw_consumption(df_o, df_s, name, opt):
    if opt == 'heating':
        col = 'DistrictHeating:Facility [J](Hourly)'
    elif opt == 'cooling':
        col = 'DistrictCooling:Facility [J](Hourly)'
    else:
        col = 'Electricity:Facility [J](Hourly)'
    
    temp_o = df_o[col] / 3.6e6
    try:
        temp_s = df_s[col] / 3.6e6       
        fig, (ax1, ax2) = plt.subplots(2, 1, sharex=True, figsize=(15, 8))
        fig.suptitle(f'Annual hourly {opt} consumption')
        ax1.plot(temp_o, color='g', label=name)
        ax2.plot(temp_s, label='standard')
        ax1.set_xticks(x_ticks)
        ax1.set_ylabel('Consumption[kWh]')
        ax1.set_xticklabels(x_label)
        plt.xlabel('Date')
        plt.ylabel('Consumption[kWh]')
        ax1.legend()
        ax2.legend()
        ax1.grid()
        ax2.grid()
        plt.show(block=False)
    except:
        plt.figure(figsize=(15, 8))
        plt.plot(temp_o, label=name)
        plt.title(f'Annual hourly {opt} consumption')
        plt.xticks(x_ticks, x_label)
        plt.xlabel('Date')
        plt.ylabel('Consumption[kWh]')
        plt.legend()
        plt.grid()
        plt.tight_layout()
        plt.show(block=False)
        
# main for GUI
def main(buildingTuple):
    targetBuilding = ""
    targetData = {}
    pseudoResultOccupancy = {}
    
    if buildingTuple == ():
        buildingTuple = ('No building is available or the internet connection fails',)
    
    # the following two variables are for different languages    
    key_text = [
        # '_option_','_help_', '_lang_','_en_','_it_','_ch_','_exit_',
        '_title1_','_target_','_select_','_local_','_browse1_','_load_','_title2_',
        '_visualization_','_daily_','_weekly_','_monthly_','_yearly_','_all_',
        '_download_','_browse2_','_json_','_title3_','_title4_', '_title5_', '_browse3_', 
        '_modify_', '_load_idf_', '_pseudo_', '_noise_', '_generate_', '_scenarios_',
        '_start_date_', '_stop_date_', '_start4simulation_', '_rest_title_', '_stop4simulation_',
        '_schedule_option_', '_option_daily_', '_option_weekly_', '_option_weekdayEnd_',
        '_option_monthly_', '_option_yearly_', '_option_all_', '_generate_schedule_',
        '_load_epw_', '_browse4_', '_checkbox1_', '_start_simulation_', '_result_text_',
        '_energy_total_', '_temperature_total_', '_heating_', '_cooling_', '_electricity_',
        '_checkbox2_'
    ] 
    dict_lang = {
        # '_option_': {'en': 'Options', 'it': 'Opzioni', 'ch': '选项'},
        '_help_': {'en': 'Help', 'it': 'Aiuto', 'ch': '帮助'},
        '_code_': {'en': 'Access code', 'it': 'Codice d\'accesso', 'ch': '访问代码'},
        '_eplus_': {'en': 'EnergyPlus Info', 'it': 'EnergyPlus Info', 'ch': 'EnergyPlus信息'},
        # '_lang_': {'en': 'Language', 'it': 'Lingua', 'ch': '语言'},
        '_en_': {'en': 'English', 'it': 'Inglese', 'ch': '英语'},
        '_it_': {'en': 'Italian', 'it': 'Italiano', 'ch': '意大利语'},
        '_ch_': {'en': 'Chinese', 'it': 'Cinese', 'ch': '中文'},
        '_exit_': {'en': 'Exit', 'it': 'Uscita', 'ch': '退出'},
        # previous are for menu that cannot be cast by key
        '_title1_': {'en': 'Building Occupancy Profile', 'it': "Profilo di occupazione dell'edificio", 'ch': '建筑占有率档案'},
        '_target_': {'en': 'Target Building', 'it': 'Edificio di destinazione', 'ch': '目标建筑'},
        '_select_': {'en': 'Select', 'it': 'Selezionare', 'ch': '选择'},
        # '_or_': {'en': 'OR', 'it': 'O', 'ch': '或'},
        '_local_': {'en': 'Load Local File', 'it': 'Carica File Locale', 'ch': '读取本地文件'},
        '_browse1_': {'en': 'Browse', 'it': 'Navigare', 'ch': '浏览'},
        '_load_': {'en': 'Load', 'it': 'Caricare', 'ch': '读取'},
        '_title2_': {'en': 'Visualization & Save', 'it': 'Visualizzazione e salvataggio', 'ch': '可视化和保存'},
        '_visualization_': {'en': 'Occupancy Visualization Options:', 'it': 'Visualizzazione dell\'occupazione opzioni:', 'ch': '占用可视化选项：'},
        '_daily_': {'en': 'Daily', 'it': 'Quotidiano', 'ch': '日'},
        '_weekly_': {'en': 'Weekly', 'it': 'Settimanalmente', 'ch': '周'},
        '_monthly_': {'en': 'Monthly', 'it': 'Mensile', 'ch': '月'},
        '_yearly_': {'en': 'Yearly', 'it': 'Annuale', 'ch': '年'},
        '_all_': {'en': 'All', 'it': 'Tutto', 'ch': '全部'},
        '_checkbox_': {'en': 'With standard', 'it': 'Con standard', 'ch': '加入标准'},
        '_download_': {'en': 'Download', 'it': 'Scarica', 'ch': '下载'},
        '_browse2_': {'en': 'Browse', 'it': 'Navigare', 'ch': '浏览'},
        '_json_': {'en': 'Save as json', 'it': 'Salva come json', 'ch': '以json形式保存'},
        '_title3_': {'en': 'Indicator:', 'it': 'Indicatore:', 'ch': '指示:'},
        '_title4_': {'en': '-- Building occupancy profile plot --', 'it': "-- Grafico del profilo di occupazione dell'edificio --", 'ch': '-- 建筑占有率曲线图 --'},
        '_title5_': {'en': 'Simulation', 'it': 'Simulazione', 'ch': '模拟'},
        '_browse3_': {'en': 'Browse', 'it': 'Navigare', 'ch': '浏览'},
        '_modify_': {'en': 'Modify Occupancy', 'it': 'Modifica Occupazione', 'ch': '更改占有率'},
        '_load_idf_': {'en': 'Load idf file', 'it': 'Carica file idf', 'ch': '读取idf文件'},
        # '_or2_': {'en': 'OR', 'it': 'O', 'ch': '或'},
        '_pseudo_': {'en': 'Pseudo Data Generation', 'it': 'Pseudo Dati Generazione', 'ch': '伪数据生成'},
        '_noise_': {'en': 'Noise level:', 'it': 'Livello di rumore:', 'ch': '干扰等级'},
        '_generate_': {'en': 'Generate', 'it': 'Creare', 'ch': '生成'},
        '_scenarios_': {'en': 'Scenarios', 'it': 'Scenari', 'ch': '场景'},
        '_start_date_': {'en': 'Start Date', 'it': 'Data d\'inizio', 'ch': '开始日期'},
        '_stop_date_': {'en': 'Stop Date', 'it': 'Data di fine', 'ch': '停止日期'},
        '_start4simulation_': {'en': 'Start Date', 'it': 'Data d\'inizio', 'ch': '开始日期'},
        '_stop4simulation_': {'en': 'Stop Date', 'it': 'Data di fine', 'ch': '停止日期'},
        '_rest_title_': {'en': 'Option for other date:', 'it': 'Opzione per altra data:', 'ch': '其他日期选项：'},
        '_schedule_option_': {'en': 'Schedule Option:', 'it': 'Opzione di pianificazione:', 'ch': '计划选项:'},
        '_option_daily_': {'en': 'Daily', 'it': 'Quotidiano', 'ch': '日'},
        '_option_weekly_': {'en': 'Weekly', 'it': 'Settimanalmente', 'ch': '周'},
        '_option_weekdayEnd_': {'en': 'Weekday/end', 'it': 'Giorno feriale/fine', 'ch': '平日/周末'},
        '_option_monthly_': {'en': 'Monthly', 'it': 'Mensile', 'ch': '月'},
        '_option_yearly_': {'en': 'Yearly', 'it': 'Annuale', 'ch': '年'},
        '_option_all_': {'en': 'All', 'it': 'Tutto', 'ch': '全部'},
        '_generate_schedule_': {'en': 'Generate Schedule', 'it': 'Genera pianificazione', 'ch': '生成时间表'},
        '_load_epw_': {'en': 'Load epw file', 'it': 'Carica file epw', 'ch': '读取epw文件'},
        '_browse4_': {'en': 'Browse', 'it': 'Navigare', 'ch': '浏览'},
        '_start_simulation_': {'en': 'Start Simulation', 'it': 'Avvia simulazione', 'ch': '开始模拟'},
        '_checkbox1_': {'en': 'With standard', 'it': 'Con standard', 'ch': '加入标准'},
        '_result_text_': {'en': 'Result visualization:  ', 'it': 'Visualizzazione dei risultati:', 'ch': '结果可视化:  '},
        '_energy_total_': {'en': 'Energy consumption', 'it': 'Consumo energetico', 'ch': '能源消耗'},
        '_temperature_total_': {'en': 'Annual temperature', 'it': 'Temperatura annuale', 'ch': '年气温'},
        '_heating_': {'en': 'Heating', 'it': 'Il riscaldamento', 'ch': '采暖'},
        '_cooling_': {'en': 'Cooling', 'it': 'Raffreddamento', 'ch': '制冷'},
        '_electricity_': {'en': 'Electricity', 'it': 'Elettricità', 'ch': '电'},
        '_checkbox2_': {'en': 'With heating&cooling', 'it': 'Con riscaldamento&raffreddamento', 'ch': '带暖气和空调'}
    }

    matplotlib.use("TkAgg")

    sg.theme('DefaultNoMoreNagging')
    # sg.theme('GrayGrayGray')
    sg.SetOptions(element_padding=(5,5)) 

    # menu for different language
    menu_def = [['Options', ['Help', 'Access code', 'EnergyPlus Info', 'Language', ['English', 'Italian', 'Chinese'], 'Exit']]] 
    menu_it = [['Opzioni', ['Aiuto', 'Codice d\'accesso', 'EnergyPlus Info', 'Lingua', ['Inglese', 'Italiano', 'Cinese'], 'Uscita']]] 
    menu_ch = [['选项', ['帮助', '访问代码', 'EnergyPlus信息', '语言', ['英语', '意大利语', '中文'], '退出']]] 
    menu4lang = {'en': menu_def, 'it': menu_it, 'ch': menu_ch}
    
    # deal with configuration file and help text
    try:
        with open(f'{current_path}/config/{f_name_config}', 'r', encoding='utf-8') as f:
            content = json.load(f)
            current_lang = content['language']
            help_text = content['help_text']
    except:
        current_lang = 'en'
        help_text = {
            'en': 'This is not available due to the missing files or other errors.',
            'it': 'Questo non è disponibile a causa di file mancanti o altri errori.',
            'ch': '由于缺少文件或其他错误，这不可用。'
        }
    
    # layouts for GUI    
    left_frame = [
        [sg.Text(dict_lang['_title1_'][current_lang], size=(30, 1), font='Helvetica 15', key='_title1_')],
        [
            sg.Text(dict_lang['_target_'][current_lang], key='_target_'),
            sg.OptionMenu(buildingTuple, size=(58, 1), key='-MENU_ADDRESS-'),
            sg.Button(dict_lang['_select_'][current_lang], key='_select_')
        ],
        # [sg.Text(dict_lang['_or_'][current_lang], size=(20, 1), justification='center', font='Helvetica 8', key='_or_')],
        [
            sg.Text(dict_lang['_local_'][current_lang], key='_local_'),
            sg.In(size=(55, 1), enable_events=True, key="-FILE-"),
            sg.FileBrowse(dict_lang['_browse1_'][current_lang], key='_browse1_', file_types=(("Json Files", "*.json"),), target='-FILE-'),
            sg.Button(dict_lang['_load_'][current_lang], key='_load_')
        ],
        # [sg.Text(dict_lang['_or2_'][current_lang], size=(20, 1), justification='center', font='Helvetica 8', key='_or2_')],
        [sg.Text('_'  * 100, pad=(5, 0), size=(75, 1))],
        [sg.Text(dict_lang['_pseudo_'][current_lang], size=(20, 1), font='Helvetica 15', key='_pseudo_')],
        [
            sg.Text(dict_lang['_scenarios_'][current_lang], key='_scenarios_'),
            sg.OptionMenu(('0, pure standard', 
                           '1, defalut noise, 0.1 for weekdays and 0.2 for weekends', 
                           '2, studio flats, lower occupancy on weekdays and higher on weekends',
                           '3, families, higher occupancy than standard',
                           '4, summer and xmas holidays, lower occupancy than standard',
                           '5, lockdown period, very high occupancy',
                           '6, customized noise, standard as mean',
                        #    '7, customized noise, customized variance and mean'
                           ), 
                            size=(60, 1), key='_scenarios_menu_'),
            sg.Button(dict_lang['_generate_'][current_lang], key='_generate_')
        ],
        [
            sg.Input(default_text = f'{date.today().year}-01-01', key='_start_date_input_', size=(15,1)),    
            sg.CalendarButton(
                dict_lang['_start_date_'][current_lang],
                # title='Pick a date for the beginning of pseudo data', 
                no_titlebar = False, 
                close_when_date_chosen = True,  
                target = '_start_date_input_', 
                format = "%Y-%m-%d",
                key = '_start_date_'
            ),
            sg.Input(default_text = f'{date.today().year}-12-31', key='_stop_date_input_', size=(15,1)),    
            sg.CalendarButton(
                dict_lang['_stop_date_'][current_lang], 
                # title='Pick a date for the beginning of pseudo data', 
                no_titlebar = False, 
                close_when_date_chosen = True,  
                target = '_stop_date_input_', 
                format = "%Y-%m-%d",
                key = '_stop_date_'
            ),
            sg.Text(dict_lang['_noise_'][current_lang], key='_noise_'),
            sg.OptionMenu(('0.1', '0.2', '0.3', '0.4', '0.5', '0.6'), default_value='0.1', size=(13, 1), key='-MENU_noise_level-'),
        ],
        [sg.Text('_'  * 100, pad=(5, 0), size=(75, 1))],
        [sg.Text(dict_lang['_title2_'][current_lang], size=(20, 1), font='Helvetica 15', key='_title2_')],
        [
            sg.Text(dict_lang['_visualization_'][current_lang], key='_visualization_'),
            sg.Button(dict_lang['_daily_'][current_lang], key='_daily_'),
            sg.Button(dict_lang['_weekly_'][current_lang], key='_weekly_'),
            sg.Button(dict_lang['_monthly_'][current_lang], key='_monthly_'),
            sg.Button(dict_lang['_yearly_'][current_lang], key='_yearly_'),
            sg.Button(dict_lang['_all_'][current_lang], key='_all_'),
            sg.Checkbox(dict_lang['_checkbox_'][current_lang], enable_events=True, key='_checkbox_')
        ],
        [
            sg.Text(dict_lang['_download_'][current_lang], key='_download_'),
            sg.In(size=(53, 1), enable_events=True, key="-FOLDER-"),
            sg.FolderBrowse(dict_lang['_browse2_'][current_lang], key='_browse2_', target='-FOLDER-'),
            sg.Button(dict_lang['_json_'][current_lang], key='_json_')
        ],
        [sg.Text('_'  * 100, pad=(5, 0), size=(75, 1))],
        [sg.Text(dict_lang['_title5_'][current_lang], size=(20, 1), font='Helvetica 15', key='_title5_')],
        [
            sg.Input(default_text = '01-01', key='_start4simulation_input_', size=(11,1)),    
            sg.CalendarButton(
                dict_lang['_start4simulation_'][current_lang],
                # title='Pick a date for the beginning of pseudo data', 
                no_titlebar = False, 
                close_when_date_chosen = True,  
                target = '_start4simulation_input_', 
                format = "%m-%d",
                key = '_start4simulation_'
            ),
            sg.Input(default_text = f'12-31', key='_stop4simulation_input_', size=(11,1)),    
            sg.CalendarButton(
                dict_lang['_stop4simulation_'][current_lang],
                # title='Pick a date for the beginning of pseudo data', 
                no_titlebar = False, 
                close_when_date_chosen = True,  
                target = '_stop4simulation_input_', 
                format = "%m-%d",
                key = '_stop4simulation_'
            ),
            sg.Text(dict_lang['_rest_title_'][current_lang], key='_rest_title_'),
            sg.OptionMenu(('daily', 'weekly', 'weekday/end', 'monthly', 'yearly', 'all'), default_value='daily', size=(13, 1), key='_oprion_rest_'),
        ],
        [
            sg.Text(dict_lang['_schedule_option_'][current_lang], key='_schedule_option_'),
            sg.Button(dict_lang['_option_daily_'][current_lang], key='_option_daily_'),
            sg.Button(dict_lang['_option_weekly_'][current_lang], key='_option_weekly_'),
            sg.Button(dict_lang['_option_weekdayEnd_'][current_lang], key='_option_weekdayEnd_'),
            sg.Button(dict_lang['_option_monthly_'][current_lang], key='_option_monthly_'),
            sg.Button(dict_lang['_option_yearly_'][current_lang], key='_option_yearly_'),
            sg.Button(dict_lang['_option_all_'][current_lang], key='_option_all_'),
            sg.Button(dict_lang['_generate_schedule_'][current_lang], key='_generate_schedule_')
        ],
        [ 
            sg.Text(dict_lang['_load_idf_'][current_lang], key='_load_idf_'),
            sg.In(size=(24, 1), enable_events=True, key="-idf_FILE-"),
            sg.FileBrowse(dict_lang['_browse3_'][current_lang], key='_browse3_', file_types=(("idf Files", "*.idf"),), target='-idf_FILE-'),
            sg.Checkbox(dict_lang['_checkbox2_'][current_lang], enable_events=True, key='_checkbox2_', default=True),
            sg.Button(dict_lang['_modify_'][current_lang], key='_modify_')
        ],
        [ 
            sg.Text(dict_lang['_load_epw_'][current_lang], key='_load_epw_'),
            sg.In(size=(31, 1), enable_events=True, key="-epw_FILE-"),
            sg.FileBrowse(dict_lang['_browse4_'][current_lang], key='_browse4_', file_types=(("epw Files", "*.epw"),), target='-epw_FILE-'),
            sg.Checkbox(dict_lang['_checkbox1_'][current_lang], enable_events=True, key='_checkbox1_', default=True),
            sg.Button(dict_lang['_start_simulation_'][current_lang], key='_start_simulation_')
        ], 
        [
            sg.Text(dict_lang['_result_text_'][current_lang], key='_result_text_'),
            sg.Button(dict_lang['_energy_total_'][current_lang], key='_energy_total_'),
            sg.Button(dict_lang['_temperature_total_'][current_lang], key='_temperature_total_'),
            sg.Button(dict_lang['_heating_'][current_lang], key='_heating_'),
            sg.Button(dict_lang['_cooling_'][current_lang], key='_cooling_'),
            sg.Button(dict_lang['_electricity_'][current_lang], key='_electricity_'),
        ],
        [sg.Text('_'  * 100, pad=(5, 0), size=(75, 1))],
        [sg.Text(dict_lang['_title3_'][current_lang], size=(20, 1), font='Helvetica 15', pad=(5,5), key='_title3_')],
        [sg.Multiline(size=(84,5), background_color='white', pad=(5, (0, 2)), key='-OUTPUT-')],
    ]

    right_frame = [
        [sg.Text(dict_lang['_title4_'][current_lang], size=(65, 1), justification='center', key='_title4_')],
        [sg.Canvas(key="-CANVAS-", pad=((15, 0), (5, 5)))]
    ]

    my_layout = [
        [sg.Menu(menu4lang[current_lang], key='menu')],
        [
            sg.Column(left_frame),
            sg.VSeparator(),
            sg.Column(right_frame)
        ]
    ]

    window = sg.Window(title=output_dict['software_title'][current_lang], layout=my_layout, margins=(10, 3))
    figure_agg = None
    
    date_dict = generateDateDict()
    output_date_option = {
        'daily': '',
        'weekly': '',
        'weekday_end': '',
        'monthly': '',
        'yearly': '',
        'all': '',
    }
    
    modified_schedule_list = []
    simulation_finished = False

    # start to listen to any actions with GUI
    while True:
        event, value = window.read()
            
        # show help
        if event == dict_lang['_help_'][current_lang]:
            # sg.popup(dict_lang['_help_'][current_lang], help_text)
            help_layout = [[sg.Multiline(help_text[current_lang], size=(90, 40), autoscroll=True, disabled=True)]]
            help_window = sg.Window(dict_lang['_help_'][current_lang], layout=help_layout, margins=(3, 3), use_default_focus=False, finalize=True, modal=True)
            block_focus(help_window)
            _, _ = help_window.read()
            help_window.close()
            
        # generate access code 
        if event == dict_lang['_code_'][current_lang]:
            emailForApp = sg.popup_get_text('Please enter the email you use for the mobile app:')
            if emailForApp == None:
                pass
            else:
                st = checkEmailAndOutput(emailForApp)
                sg.popup('This is your code for accessing more information in app:', st)
        
        # check info about e+:
        if event == dict_lang['_eplus_'][current_lang]:
            sg.popup('EnergyPlus information', 'Version: 9.4.0\nidd Location: C:/EnergyPlusV9-4-0/Energy+.idd\n\nPlease make sure the location is correct and the version of your idf file matches.')
        
        # deal with language 
        if event == dict_lang['_en_'][current_lang]:
            window['menu'].update(menu_def)
            current_lang = 'en'
            window.Element('_checkbox_').Update(text=dict_lang['_checkbox_'][current_lang])
            for key in key_text:
                window.Element(key).Update(dict_lang[key][current_lang])
            change_configuration(current_lang, help_text)
        if event == dict_lang['_it_'][current_lang]:
            window['menu'].update(menu_it)
            current_lang = 'it'
            window.Element('_checkbox_').Update(text=dict_lang['_checkbox_'][current_lang])
            for key in key_text:
                window.Element(key).Update(dict_lang[key][current_lang])
            change_configuration(current_lang, help_text)
        if event == dict_lang['_ch_'][current_lang]:
            window['menu'].update(menu_ch)
            current_lang = 'ch'
            window.Element('_checkbox_').Update(text=dict_lang['_checkbox_'][current_lang])
            for key in key_text:
                window.Element(key).Update(dict_lang[key][current_lang])
            change_configuration(current_lang, help_text)
        
        # exit  
        if event == sg.WIN_CLOSED or event == dict_lang['_exit_'][current_lang]:
            break
        
        # choose target building from a list download from database
        if event == '_select_':
            if value['-MENU_ADDRESS-'] == "":
                window['-OUTPUT-'].update('You have selected nothing.')
            else:
                # window['-OUTPUT-'].update(f"you have selected {value['-MENU_ADDRESS-']}") 
                window['-OUTPUT-'].update('DOWNLOADING, please wait...')
                window.Refresh()
                
                targetBuilding = value['-MENU_ADDRESS-']
                targetData = getAllResultsForTargetBuilding(targetBuilding, window['-OUTPUT-'], current_lang)
                targetData['jsonFileForOccupancyProfile'] = True
                targetData['targetBuilding'] = targetBuilding
                pseudoResultOccupancy = {}
                
        # load json file with occupancy profile
        if event == '_load_':
            jsonDirectory = value['-FILE-']
            if jsonDirectory:
                try:
                    with open(jsonDirectory, 'r') as f:
                        tempData = json.load(f)
                    if tempData['jsonFileForOccupancyProfile']:
                        pseudoResultOccupancy = tempData.copy()
                        scenario = pseudoResultOccupancy['targetBuilding']
                        targetData = {}
                        window['-OUTPUT-'].update('You have successfully loaded the json file.')
                    else:
                        window['-OUTPUT-'].update('The json file is not the one for occupancy profile.')
                except:
                    window['-OUTPUT-'].update('The file is not correct or damaged or the format is wrong.')
            else:
                window['-OUTPUT-'].update('Please choose your json file for occupancy profile.')
                
        # generate fake data for different scenarios
        if event == '_generate_':
            if value['_scenarios_menu_'] == '':
                window['-OUTPUT-'].update('Please select your scenario')
            else:
                if (value['_start_date_input_'] > value['_stop_date_input_']):
                    window['-OUTPUT-'].update("The stop time should come after start time.")
                else:
                    window['-OUTPUT-'].update('GENERATING, please wait...')
                    window.Refresh()
                    
                    window['-OUTPUT-'].update(f"you have selected and generated: {value['_scenarios_menu_']}")
                    scenario = int(value['_scenarios_menu_'][0])
                    startDate = value['_start_date_input_'] + '-00'
                    stopDate = value['_stop_date_input_'] + '-23'
                    noise_lvl = float(value['-MENU_noise_level-'])
                    
                    pseudoData = pseudoDataGeneration(startDate, stopDate, 1)
                    pseudoResultOccupancy = pseudoData.Gene_Pesudo(scenario, 0, noise_lvl)
                    pseudoResultOccupancy['jsonFileForOccupancyProfile'] = True
                    pseudoResultOccupancy['targetBuilding'] = f'pesudoData scen_{scenario}'
                    targetData = {}
                    # pseudoDataGeneration('start time','end time',time interval if needed).Gene_Pesudo(scenarios,mean if needed,variance is needed)
                        # list of scenarios:
                        # 0 = pure standard
                        # 1 = defalut noise, variance = 0.1 for weekdays and 0.2 for weekends
                        # 2 = studio flats, lower occupants on weekdays and higher on weekends
                        # 3 = families, higher occupants than standard
                        # 4 = summer and xmas holidays, lower occupants than standard
                        # 5 = lockdown period, very high occupants
                        # 6 = customized variance noise, standard as mean
                        # 7 = customized noise, customized variance and mean (currently not needed)
                
        # visualize daily occupancy profile        
        if event == '_daily_':
            if (not targetData) and (not pseudoResultOccupancy):
                window['-OUTPUT-'].update("You haven't selected any building or pesudo data yet.")
            else:
                x, y = [], []
                if targetData:
                    if targetData['results_availability']['daily_available']:    
                        docs = targetData['daily_data']
                        for doc in docs:
                            x.append(doc['interval'])
                            y.append(doc['occupancy'])
                        fig = drawDailyPlot(x, y, value['_checkbox_'], targetBuilding)
                        window['-OUTPUT-'].update('The plot is on the right side.')
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
                elif pseudoResultOccupancy:
                    if pseudoResultOccupancy['results_availability']['daily_available']:
                        for elem in pseudoResultOccupancy['daily_data']:
                            x.append(elem['interval'])
                            y.append(elem['occupancy'])
                        window['-OUTPUT-'].update('The plot is on the right side.')
                        try:
                            titleInput = f"pesudoData scen_{scenario-1+1}"
                        except:
                            titleInput = scenario
                        fig = drawDailyPlot(x, y, value['_checkbox_'], titleInput)
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
                
                try:
                    if figure_agg:
                        figure_agg.get_tk_widget().forget()
                        plt.close('all')
                    figure_agg = draw_figure(window["-CANVAS-"].TKCanvas, fig)
                except:
                    window['-OUTPUT-'].update('Something is wrong, please try it later.')
        
        # visualize weekly occupancy profile including weekday and weekend data
        if event == '_weekly_':
            if (not targetData) and (not pseudoResultOccupancy):
                window['-OUTPUT-'].update("You haven't selected any building or pesudo data yet.")
            else:
                x_all, y_all = [], []
                x_weekday, y_weekday = [], []
                x_weekend, y_weekend = [], []
                if targetData:
                    if targetData['results_availability']["weekly_available"]:
                        for elem in targetData['weekly_data']:
                            x_all.append(elem['interval'][2:])
                            y_all.append(elem['occupancy'])
                        for elem in targetData['weekdaysAndWeekends_data']:
                            if elem['interval'][0:7] == 'weekday':
                                x_weekday.append(elem['interval'])
                                y_weekday.append(elem['occupancy'])
                            else:
                                x_weekend.append(elem['interval'])
                                y_weekend.append(elem['occupancy'])
                        window['-OUTPUT-'].update('The plot will be poped out.')
                        drawWeeklyPlot(x_all, y_all, x_weekday, y_weekday, x_weekend, y_weekend, value['_checkbox_'], targetBuilding)
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
                else:
                    if pseudoResultOccupancy['results_availability']['weekly_available']:
                        for elem in pseudoResultOccupancy['weekly_data']:
                            x_all.append(elem['interval'][2:])
                            y_all.append(elem['occupancy'])
                        for elem in pseudoResultOccupancy['weekdaysAndWeekends_data']:
                            if elem['interval'][0:7] == 'weekday':
                                x_weekday.append(elem['interval'])
                                y_weekday.append(elem['occupancy'])
                            else:
                                x_weekend.append(elem['interval'])
                                y_weekend.append(elem['occupancy'])
                        window['-OUTPUT-'].update('The plot will be poped out.')
                        try:
                            titleInput = f"pesudoData scen_{scenario-1+1}"
                        except:
                            titleInput = scenario
                        drawWeeklyPlot(x_all, y_all, x_weekday, y_weekday, x_weekend, y_weekend, value['_checkbox_'], titleInput)
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
                    
        # visualize monthly occupancy profile     
        if event == '_monthly_':
            if (not targetData) and (not pseudoResultOccupancy):
                window['-OUTPUT-'].update("You haven't selected any building or pesudo data yet.")
            else:
                occupancyValuesInput = {'Jan':[], 'Feb':[], 'Mar':[], 'Apr':[], 'May':[], 'Jun':[], 'Jul':[], 'Aug':[], 'Sep':[], 'Oct':[], 'Nov':[], 'Dec':[]}
                if targetData:
                    if targetData['results_availability']["monthly_12_available"]:
                        docs = targetData['monthly_data']
                        for doc in docs:
                            occupancyValuesInput[doc['interval'][0:3]].append(doc['occupancy'])
                        drawMonthlyPlot(occupancyValuesInput, value['_checkbox_'], targetBuilding)
                        window['-OUTPUT-'].update('The plot will be poped out.')
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
                else:
                    if pseudoResultOccupancy['results_availability']['monthly_12_available']:
                        for elem in pseudoResultOccupancy['monthly_data']:
                            occupancyValuesInput[elem['interval'][0:3]].append(elem['occupancy'])
                        try:
                            titleInput = f"pesudoData scen_{scenario-1+1}"
                        except:
                            titleInput = scenario
                        drawMonthlyPlot(occupancyValuesInput, value['_checkbox_'], titleInput)
                        window['-OUTPUT-'].update('The plot will be poped out.')
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')

        # visualize yearly occupancy profile 
        if event == '_yearly_':
            if (not targetData) and (not pseudoResultOccupancy):
                window['-OUTPUT-'].update("You haven't selected any building or pesudo data yet.")
            else:
                x, y = [], []
                if targetData:
                    if targetData['results_availability']["yearly_available"]:
                        docs = targetData['yearly_data']
                        tempYear = ''
                        btnList = []
                        for doc in docs:
                            tempId = doc['interval'][0:4]
                            if tempYear != tempId:
                                tempYear = tempId
                                btnList.append(sg.Button(tempYear, key=tempYear))
                        selectedYear = popupYearSelection(btnList)
                        window['-OUTPUT-'].update(f"You have selected the year of {selectedYear} and the plot is on the right side.")
                        docs = targetData['yearly_data']
                        x = []
                        y = []
                        for doc in docs:
                            if (doc['interval'][0:4] == selectedYear):
                                x.append(doc['interval'])
                                y.append(doc['occupancy'])
                        fig = drawYearlyPlot(x, y, selectedYear, value['_checkbox_'], targetBuilding)
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
                else:
                    if pseudoResultOccupancy['results_availability']['yearly_available']:
                        tempYear = ''
                        btnList = []
                        for elem in pseudoResultOccupancy['yearly_data']:
                            tempId = elem['interval'][0:4]
                            if tempYear != tempId:
                                tempYear = tempId
                                btnList.append(sg.Button(tempYear, key=tempYear))
                        selectedYear = popupYearSelection(btnList)
                        window['-OUTPUT-'].update(f"You have selected the year of {selectedYear} and the plot is on the right side.")
                        for elem in pseudoResultOccupancy['yearly_data']:
                            if elem['interval'][0:4] == selectedYear:
                                x.append(elem['interval'])
                                y.append(elem['occupancy'])
                        try:
                            titleInput = f"pesudoData scen_{scenario-1+1}"
                        except:
                            titleInput = scenario
                        fig = drawYearlyPlot(x, y, selectedYear, value['_checkbox_'], titleInput)
                                
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
                
                try:
                    if figure_agg:
                        figure_agg.get_tk_widget().forget()
                        plt.close('all')
                    figure_agg = draw_figure(window["-CANVAS-"].TKCanvas, fig)
                except :
                    window['-OUTPUT-'].update('Something is wrong, please try it later.')
        
        # visualize all occupancy profile per hour
        if event == '_all_':
            if (not targetData) and (not pseudoResultOccupancy):
                window['-OUTPUT-'].update("You haven't selected any building or pesudo data yet.")
            else:
                x, y = [], []
                if targetData:
                    if targetData['results_availability']["total_available"]:
                        docs = targetData['total_data']
                        for doc in docs:
                            x.append(doc['interval'])
                            if doc['occupancy']<0:
                                y.append(-0.1)
                            else:
                                y.append(doc['occupancy'])
                        window['-OUTPUT-'].update('The plot will be poped out.')
                        drawAllPlot(x, y, targetBuilding)
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
                else:
                    if pseudoResultOccupancy['results_availability']['total_available']:
                        for elem in pseudoResultOccupancy['total_data']:
                            x.append(elem['interval'])
                            if elem['occupancy']<0:
                                y.append(-0.1)
                            else:
                                y.append(elem['occupancy'])
                        window['-OUTPUT-'].update('The plot will be poped out.')
                        try:
                            titleInput = f"pesudoData scen_{scenario-1+1}"
                        except:
                            titleInput = scenario
                        drawAllPlot(x, y, titleInput)
                    else:
                        window['-OUTPUT-'].update('The results are not available yet.')
        
        # download the data as json   
        if event == '_json_':
            directory = value['-FOLDER-']
            if targetData:
                if directory:
                    window['-OUTPUT-'].update(f"You haven successfully download and save the occupancy data for {targetBuilding}.")
                    targetData['jsonFileForOccupancyProfile'] = True
                    targetData['targetBuilding'] = targetBuilding
                    with open(f"{directory}/{targetBuilding}OccupancyData.json","w") as f:
                        json.dump(targetData, f, indent=4)
                else:
                    window['-OUTPUT-'].update('Please select your folder to download')
            elif pseudoResultOccupancy:
                if directory:
                    window['-OUTPUT-'].update(f"You haven successfully save the pesudo occupancy data.")
                    pseudoResultOccupancy['jsonFileForOccupancyProfile'] = True
                    pseudoResultOccupancy['targetBuilding'] = f'pesudoData scen_{scenario}'
                    with open(f"{directory}/pseudoDataForOccupancyProfileOfScenario_{scenario}.json","w") as f:
                        json.dump(pseudoResultOccupancy, f, indent=4)
                else:
                    window['-OUTPUT-'].update('Please select your folder to download') 
            else:
                window['-OUTPUT-'].update("You haven't selected any building or pesudo data yet.")
                
        # generate schedule based on daily occupancy        
        if event == '_option_daily_':
            if targetData or pseudoResultOccupancy:
                startDate4simul = value['_start4simulation_input_']
                stopDate4simul = value['_stop4simulation_input_']
                if startDate4simul > stopDate4simul:
                    window['-OUTPUT-'].update('The stop time should come after start time.')
                else:
                    if checkAlreadySelectedDate(date_dict, startDate4simul, stopDate4simul):
                        changeDateDict(date_dict, startDate4simul, stopDate4simul)
                        output_date_option['daily'] += f'[{startDate4simul} - {stopDate4simul}] '
                        window['-OUTPUT-'].update(
                            f"You have already selected {'nothing ' if not output_date_option['daily'] else output_date_option['daily']}for daily, " + 
                            f"{'nothing ' if not output_date_option['weekly'] else output_date_option['weekly']}for weekly, " + 
                            f"{'nothing ' if not output_date_option['weekday_end'] else output_date_option['weekday_end']}for weekday and weeked, " +
                            f"{'nothing ' if not output_date_option['monthly'] else output_date_option['monthly']}for monthly, " +
                            f"{'nothing ' if not output_date_option['yearly'] else output_date_option['yearly']}for yearly, " +
                            f"{'nothing ' if not output_date_option['all'] else output_date_option['all']}for all collected data, " +
                            f"and the rest is {value['_oprion_rest_']}." 
                        )
                        temp_list = [
                            f'{startDate4simul[3:]} {months_list[int(startDate4simul[0:2])-1]}',
                            f'{stopDate4simul[3:]} {months_list[int(stopDate4simul[0:2])-1]}',
                            'daily'
                        ]
                        modified_schedule_list.append(temp_list)
                    else:
                        window['-OUTPUT-'].update('The dates you select are already set with other schedule options.')
            else:
                window['-OUTPUT-'].update('You haven\'t selected any occupancy yet.')
        
        # generate schedule based on weekly occupancy     
        if event == '_option_weekly_':
            if targetData or pseudoResultOccupancy:
                startDate4simul = value['_start4simulation_input_']
                stopDate4simul = value['_stop4simulation_input_']
                if startDate4simul > stopDate4simul:
                    window['-OUTPUT-'].update('The stop time should come after start time.')
                else:
                    if checkAlreadySelectedDate(date_dict, startDate4simul, stopDate4simul):
                        changeDateDict(date_dict, startDate4simul, stopDate4simul)
                        output_date_option['weekly'] += f'[{startDate4simul} - {stopDate4simul}] '
                        window['-OUTPUT-'].update(
                            f"You have already selected {'nothing ' if not output_date_option['daily'] else output_date_option['daily']}for daily, " + 
                            f"{'nothing ' if not output_date_option['weekly'] else output_date_option['weekly']}for weekly, " + 
                            f"{'nothing ' if not output_date_option['weekday_end'] else output_date_option['weekday_end']}for weekday and weeked, " +
                            f"{'nothing ' if not output_date_option['monthly'] else output_date_option['monthly']}for monthly, " +
                            f"{'nothing ' if not output_date_option['yearly'] else output_date_option['yearly']}for yearly, " +
                            f"{'nothing ' if not output_date_option['all'] else output_date_option['all']}for all collected data, " +
                            f"and the rest is {value['_oprion_rest_']}." 
                        )
                        temp_list = [
                            f'{startDate4simul[3:]} {months_list[int(startDate4simul[0:2])-1]}',
                            f'{stopDate4simul[3:]} {months_list[int(stopDate4simul[0:2])-1]}',
                            'weekly'
                        ]
                        modified_schedule_list.append(temp_list)
                    else:
                        window['-OUTPUT-'].update('The dates you select are already set with other schedule options.')
            else:
                window['-OUTPUT-'].update('You haven\'t selected any occupancy yet.')
        
        # generate schedule based on weekday and weekend occupancy                
        if event == '_option_weekdayEnd_':
            if targetData or pseudoResultOccupancy:
                startDate4simul = value['_start4simulation_input_']
                stopDate4simul = value['_stop4simulation_input_']
                if startDate4simul > stopDate4simul:
                    window['-OUTPUT-'].update('The stop time should come after start time.')
                else:
                    if checkAlreadySelectedDate(date_dict, startDate4simul, stopDate4simul):
                        changeDateDict(date_dict, startDate4simul, stopDate4simul)
                        output_date_option['weekday_end'] += f'[{startDate4simul} - {stopDate4simul}] '
                        window['-OUTPUT-'].update(
                            f"You have already selected {'nothing ' if not output_date_option['daily'] else output_date_option['daily']}for daily, " + 
                            f"{'nothing ' if not output_date_option['weekly'] else output_date_option['weekly']}for weekly, " + 
                            f"{'nothing ' if not output_date_option['weekday_end'] else output_date_option['weekday_end']}for weekday and weeked, " +
                            f"{'nothing ' if not output_date_option['monthly'] else output_date_option['monthly']}for monthly, " +
                            f"{'nothing ' if not output_date_option['yearly'] else output_date_option['yearly']}for yearly, " +
                            f"{'nothing ' if not output_date_option['all'] else output_date_option['all']}for all collected data, " +
                            f"and the rest is {value['_oprion_rest_']}." 
                        )
                        temp_list = [
                            f'{startDate4simul[3:]} {months_list[int(startDate4simul[0:2])-1]}',
                            f'{stopDate4simul[3:]} {months_list[int(stopDate4simul[0:2])-1]}',
                            'weekdaysAndWeekends'
                        ]
                        modified_schedule_list.append(temp_list)
                    else:
                        window['-OUTPUT-'].update('The dates you select are already set with other schedule options.')
            else:
                window['-OUTPUT-'].update('You haven\'t selected any occupancy yet.')
                
        # generate schedule based on monthly occupancy                
        if event == '_option_monthly_':
            if targetData or pseudoResultOccupancy:
                startDate4simul = value['_start4simulation_input_']
                stopDate4simul = value['_stop4simulation_input_']
                if startDate4simul > stopDate4simul:
                    window['-OUTPUT-'].update('The stop time should come after start time.')
                else:
                    if checkAlreadySelectedDate(date_dict, startDate4simul, stopDate4simul):
                        changeDateDict(date_dict, startDate4simul, stopDate4simul)
                        output_date_option['monthly'] += f'[{startDate4simul} - {stopDate4simul}] '
                        window['-OUTPUT-'].update(
                            f"You have already selected {'nothing ' if not output_date_option['daily'] else output_date_option['daily']}for daily, " + 
                            f"{'nothing ' if not output_date_option['weekly'] else output_date_option['weekly']}for weekly, " + 
                            f"{'nothing ' if not output_date_option['weekday_end'] else output_date_option['weekday_end']}for weekday and weeked, " +
                            f"{'nothing ' if not output_date_option['monthly'] else output_date_option['monthly']}for monthly, " +
                            f"{'nothing ' if not output_date_option['yearly'] else output_date_option['yearly']}for yearly, " +
                            f"{'nothing ' if not output_date_option['all'] else output_date_option['all']}for all collected data, " +
                            f"and the rest is {value['_oprion_rest_']}." 
                        )
                        temp_list = [
                            f'{startDate4simul[3:]} {months_list[int(startDate4simul[0:2])-1]}',
                            f'{stopDate4simul[3:]} {months_list[int(stopDate4simul[0:2])-1]}',
                            'monthly'
                        ]
                        modified_schedule_list.append(temp_list)
                    else:
                        window['-OUTPUT-'].update('The dates you select are already set with other schedule options.')
            else:
                window['-OUTPUT-'].update('You haven\'t selected any occupancy yet.')
                
        # generate schedule based on yearly occupancy                
        if event == '_option_yearly_':
            if targetData or pseudoResultOccupancy:
                availability = True
                if targetData:
                    if targetData['results_availability']["yearly_available"]:
                        docs = targetData['yearly_data']
                        tempYear = ''
                        btnList = []
                        for doc in docs:
                            tempId = doc['interval'][0:4]
                            if tempYear != tempId:
                                tempYear = tempId
                                btnList.append(sg.Button(tempYear, key=tempYear))
                        selectedYear = popupYearSelection(btnList)
                    else:
                        availability = False
                        window['-OUTPUT-'].update('The results for yearly are not available yet.')
                else:
                    if pseudoResultOccupancy['results_availability']['yearly_available']:
                        tempYear = ''
                        btnList = []
                        for elem in pseudoResultOccupancy['yearly_data']:
                            tempId = elem['interval'][0:4]
                            if tempYear != tempId:
                                tempYear = tempId
                                btnList.append(sg.Button(tempYear, key=tempYear))
                        selectedYear = popupYearSelection(btnList)
                    else:
                        availability = False
                        window['-OUTPUT-'].update('The results for yearly are not available yet.')
                
                if availability:
                    startDate4simul = value['_start4simulation_input_']
                    stopDate4simul = value['_stop4simulation_input_']
                    if startDate4simul > stopDate4simul:
                        window['-OUTPUT-'].update('The stop time should come after start time.')
                    else:
                        if checkAlreadySelectedDate(date_dict, startDate4simul, stopDate4simul):
                            changeDateDict(date_dict, startDate4simul, stopDate4simul)
                            output_date_option['yearly'] += f'[{startDate4simul} - {stopDate4simul} in {selectedYear}] '
                            window['-OUTPUT-'].update(
                                f"You have already selected {'nothing ' if not output_date_option['daily'] else output_date_option['daily']}for daily, " + 
                                f"{'nothing ' if not output_date_option['weekly'] else output_date_option['weekly']}for weekly, " + 
                                f"{'nothing ' if not output_date_option['weekday_end'] else output_date_option['weekday_end']}for weekday and weeked, " +
                                f"{'nothing ' if not output_date_option['monthly'] else output_date_option['monthly']}for monthly, " +
                                f"{'nothing ' if not output_date_option['yearly'] else output_date_option['yearly']}for yearly, " +
                                f"{'nothing ' if not output_date_option['all'] else output_date_option['all']}for all collected data, " +
                                f"and the rest is {value['_oprion_rest_']}." 
                            )
                            temp_list = [
                                f'{startDate4simul[3:]} {months_list[int(startDate4simul[0:2])-1]}',
                                f'{stopDate4simul[3:]} {months_list[int(stopDate4simul[0:2])-1]}',
                                'yearly',
                                selectedYear
                            ]
                            modified_schedule_list.append(temp_list)
                        else:
                            window['-OUTPUT-'].update('The dates you select are already set with other schedule options.')
            else:
                window['-OUTPUT-'].update('You haven\'t selected any occupancy yet.')
                
        
        # generate schedule based on all collected occupancy (not needed)               
        if event == '_option_all_':
            window['-OUTPUT-'].update('This option is not supported')
            # if targetData or pseudoResultOccupancy:
            #     startDate4simul = value['_start4simulation_input_']
            #     stopDate4simul = value['_stop4simulation_input_']
            #     if startDate4simul > stopDate4simul:
            #         window['-OUTPUT-'].update('The stop time should come after start time.')
            #     else:
            #         if checkAlreadySelectedDate(date_dict, startDate4simul, stopDate4simul):
            #             changeDateDict(date_dict, startDate4simul, stopDate4simul)
            #             output_date_option['all'] += f'[{startDate4simul} - {stopDate4simul}] '
            #             window['-OUTPUT-'].update(
            #                 f"You have already selected {'nothing ' if not output_date_option['daily'] else output_date_option['daily']}for daily, " + 
            #                 f"{'nothing ' if not output_date_option['weekly'] else output_date_option['weekly']}for weekly, " + 
            #                 f"{'nothing ' if not output_date_option['weekday_end'] else output_date_option['weekday_end']}for weekday and weeked, " +
            #                 f"{'nothing ' if not output_date_option['monthly'] else output_date_option['monthly']}for monthly, " +
            #                 f"{'nothing ' if not output_date_option['yearly'] else output_date_option['yearly']}for yearly, " +
            #                 f"{'nothing ' if not output_date_option['all'] else output_date_option['all']}for all collected data, " +
            #                 f"and the rest is {value['_oprion_rest_']}." 
            #             )
            #         else:
            #             window['-OUTPUT-'].update('The dates you select are already set with other schedule options.')
            # else:
            #     window['-OUTPUT-'].update('You haven\'t selected any occupancy yet.')
                        
        # generate schedules
        if event == '_generate_schedule_':
            if targetData or pseudoResultOccupancy:
                num_check = 0
                for key in date_dict.keys():
                    if date_dict[key]:
                        num_check += 1
                        
                if num_check != 365:
                    if value['_oprion_rest_'] == 'yearly':
                        if targetData or pseudoResultOccupancy:
                            availability = True
                            if targetData:
                                if targetData['results_availability']["yearly_available"]:
                                    docs = targetData['yearly_data']
                                    tempYear = ''
                                    btnList = []
                                    for doc in docs:
                                        tempId = doc['interval'][0:4]
                                        if tempYear != tempId:
                                            tempYear = tempId
                                            btnList.append(sg.Button(tempYear, key=tempYear))
                                    selectedYear = popupYearSelection(btnList)
                                else:
                                    availability = False
                                    window['-OUTPUT-'].update('The results for yearly are not available yet, please choose another option for the remaining dates.')
                            else:
                                if pseudoResultOccupancy['results_availability']['yearly_available']:
                                    tempYear = ''
                                    btnList = []
                                    for elem in pseudoResultOccupancy['yearly_data']:
                                        tempId = elem['interval'][0:4]
                                        if tempYear != tempId:
                                            tempYear = tempId
                                            btnList.append(sg.Button(tempYear, key=tempYear))
                                    selectedYear = popupYearSelection(btnList)
                                else:
                                    availability = False
                                    window['-OUTPUT-'].update('The results for yearly are not available yet, please choose another option for the remaining dates.')
                            
                            if availability:
                                window['-OUTPUT-'].update(
                                    'You have generated the schedule based on following options:\n' + 
                                    f"You have already selected {'nothing ' if not output_date_option['daily'] else output_date_option['daily']}for daily, " + 
                                    f"{'nothing ' if not output_date_option['weekly'] else output_date_option['weekly']}for weekly, " + 
                                    f"{'nothing ' if not output_date_option['weekday_end'] else output_date_option['weekday_end']}for weekday and weeked, " +
                                    f"{'nothing ' if not output_date_option['monthly'] else output_date_option['monthly']}for monthly, " +
                                    f"{'nothing ' if not output_date_option['yearly'] else output_date_option['yearly']}for yearly, " +
                                    f"{'nothing ' if not output_date_option['all'] else output_date_option['all']}for all collected data, " +
                                    f"and the rest is {value['_oprion_rest_']} in {selectedYear}.\n" +  
                                    'The options have been cleared and if you want to generate another one, please select options again.'
                                )
                    else:
                        window['-OUTPUT-'].update(
                            'You have generated the schedule based on following options:\n' + 
                            f"You have already selected {'nothing ' if not output_date_option['daily'] else output_date_option['daily']}for daily, " + 
                            f"{'nothing ' if not output_date_option['weekly'] else output_date_option['weekly']}for weekly, " + 
                            f"{'nothing ' if not output_date_option['weekday_end'] else output_date_option['weekday_end']}for weekday and weeked, " +
                            f"{'nothing ' if not output_date_option['monthly'] else output_date_option['monthly']}for monthly, " +
                            f"{'nothing ' if not output_date_option['yearly'] else output_date_option['yearly']}for yearly, " +
                            f"{'nothing ' if not output_date_option['all'] else output_date_option['all']}for all collected data, " +
                            f"and the rest is {value['_oprion_rest_']}.\n" +  
                            'The options have been cleared and if you want to generate another one, please select options again.'
                        )
                        
                    option_rest = value['_oprion_rest_']
                    if value['_oprion_rest_'] == "weekday/end":
                        option_rest = "weekdaysAndWeekends"
                    elif value['_oprion_rest_'] == "all":
                        option_rest = "daily"
                    
                    if targetData:
                        if value['_oprion_rest_'] == 'yearly':
                            schedule_generator = OccuToSchedule(targetData, option_rest, year=selectedYear)
                        else:
                            schedule_generator = OccuToSchedule(targetData, option_rest)
                    else:
                        if value['_oprion_rest_'] == 'yearly':
                            schedule_generator = OccuToSchedule(pseudoResultOccupancy, option_rest, year=selectedYear)
                        else:
                            schedule_generator = OccuToSchedule(pseudoResultOccupancy, option_rest)
                            
                    schedule_generator.mix_schedule(modified_schedule_list)
                    schedule_generator.create_HCschedule(value['_checkbox2_'])
                        
                    date_dict = generateDateDict()
                    output_date_option = {
                        'daily': '',
                        'weekly': '',
                        'weekday_end': '',
                        'monthly': '',
                        'yearly': '',
                        'all': '',
                    }
                    modified_schedule_list = []
                else:
                    window['-OUTPUT-'].update('You haven\'t specify any schedule yet.')
            else:
                window['-OUTPUT-'].update('You haven\'t selected any occupancy yet.')
        
        # modify idf with occupancy
        if event == '_modify_':
            if targetData or pseudoResultOccupancy:
                directory_idf = value['-idf_FILE-'] 
                directory_epw = value['-epw_FILE-']
                if (not directory_idf) or (not directory_epw):
                    window['-OUTPUT-'].update('Please select the idf file.')
                else:
                    window['-OUTPUT-'].update('The modification is RUNNING, please wait...')
                    window.Refresh()
                    
                    try:
                        if targetData:
                            input_name = targetData['targetBuilding'] 
                        else:
                            input_name = pseudoResultOccupancy['targetBuilding']
                        object_name = 'Dwell_DomCommonAreas_Occ'
                        if value['_checkbox1_']:
                            Simulation(directory_idf, object_name, directory_epw, input_name, with_heating_cooling=value['_checkbox2_']).modify_standardIDF()
                        Simulation(directory_idf, object_name, directory_epw, input_name, with_heating_cooling=value['_checkbox2_']).modify_IDF()
                        window['-OUTPUT-'].update('Modification finished.')
                    except:
                        window['-OUTPUT-'].update('Modification failed. \nThere is something wrong, please make sure the files are complete, the version is correct and the locations for both files and Energyplus is correct.')
            else:
                window['-OUTPUT-'].update('Please select your target building or pseudo data or load files.')
                
        # perform simulation         
        if event == '_start_simulation_':
            if targetData or pseudoResultOccupancy:
                directory_idf = value['-idf_FILE-'] 
                directory_epw = value['-epw_FILE-']
                if (not directory_idf) or (not directory_epw):
                    window['-OUTPUT-'].update('Please select the idf file and epw file for simulation.')
                else:
                    window['-OUTPUT-'].update('The simulation is RUNNING, please wait...')
                    window.Refresh()
                    
                    # start simulation and handle possible errors
                    if targetData:
                        input_name = targetData['targetBuilding'] 
                    else:
                        input_name = pseudoResultOccupancy['targetBuilding']
                    
                    try:
                        object_name = 'Dwell_DomCommonAreas_Occ'
                        Simulation(directory_idf, object_name, directory_epw, input_name, value['_checkbox1_']).run_eppy()
                        window['-OUTPUT-'].update('Simulation finished.')
                        explore(f'./output/{input_name}')
                        simulation_finished = True
                    except:
                        window['-OUTPUT-'].update('Simulation failed. \nThere is something wrong, please make sure the files are complete, the version is correct and the locations for both files and Energyplus is correct.')
            else:
                window['-OUTPUT-'].update('Please select your target building or pseudo data or load files.')
                
        # show result of simulation, total energy consumption
        if event == '_energy_total_':
            if simulation_finished:
                if targetData:
                    input_name = targetData['targetBuilding'] 
                else:
                    input_name = pseudoResultOccupancy['targetBuilding']
                df_my_schedule = pd.read_csv(f'./output/{input_name}/eppy_outputs/eplusout.csv')
                try:
                    df_standard = pd.read_csv(f'./output/{input_name}/eppy_outputs_standard/eplusout.csv')
                except:
                    df_standard = None
                    
                fig = draw_total_consumption(df_my_schedule, df_standard, input_name)
                try:
                    if figure_agg:
                        figure_agg.get_tk_widget().forget()
                        plt.close('all')
                    figure_agg = draw_figure(window["-CANVAS-"].TKCanvas, fig)
                    window['-OUTPUT-'].update('The plot is on the right side.')
                except:
                    window['-OUTPUT-'].update('Something is wrong, please try it later.')
            else:
                window['-OUTPUT-'].update('You need to run the simulation to get the result.')
        
        # show result of simulation, annual hourly temperature
        if event == '_temperature_total_':
            if simulation_finished:
                if targetData:
                    input_name = targetData['targetBuilding'] 
                else:
                    input_name = pseudoResultOccupancy['targetBuilding']
                df_my_schedule = pd.read_csv(f'./output/{input_name}/eppy_outputs/eplusout.csv')
                try:
                    df_standard = pd.read_csv(f'./output/{input_name}/eppy_outputs_standard/eplusout.csv')
                except:
                    df_standard = None
                    
                draw_temperature(df_my_schedule, df_standard, input_name)
            else:
                window['-OUTPUT-'].update('You need to run the simulation to get the result.')
        
        # show result of simulation, annual hourly heating consumption  
        if event == '_heating_':
            if simulation_finished:
                if targetData:
                    input_name = targetData['targetBuilding'] 
                else:
                    input_name = pseudoResultOccupancy['targetBuilding']
                df_my_schedule = pd.read_csv(f'./output/{input_name}/eppy_outputs/eplusout.csv')
                try:
                    df_standard = pd.read_csv(f'./output/{input_name}/eppy_outputs_standard/eplusout.csv')
                except:
                    df_standard = None
                    
                draw_consumption(df_my_schedule, df_standard, input_name, 'heating')
            else:
                window['-OUTPUT-'].update('You need to run the simulation to get the result.')
        
        # show result of simulation, annual hourly cooling consumption     
        if event == '_cooling_':
            if simulation_finished:
                if targetData:
                    input_name = targetData['targetBuilding'] 
                else:
                    input_name = pseudoResultOccupancy['targetBuilding']
                df_my_schedule = pd.read_csv(f'./output/{input_name}/eppy_outputs/eplusout.csv')
                try:
                    df_standard = pd.read_csv(f'./output/{input_name}/eppy_outputs_standard/eplusout.csv')
                except:
                    df_standard = None
                    
                draw_consumption(df_my_schedule, df_standard, input_name, 'cooling')
            else:
                window['-OUTPUT-'].update('You need to run the simulation to get the result.')
        
        # show result of simulation, annual hourly electricity consumption     
        if event == '_electricity_':
            if simulation_finished:
                if targetData:
                    input_name = targetData['targetBuilding'] 
                else:
                    input_name = pseudoResultOccupancy['targetBuilding']
                df_my_schedule = pd.read_csv(f'./output/{input_name}/eppy_outputs/eplusout.csv')
                try:
                    df_standard = pd.read_csv(f'./output/{input_name}/eppy_outputs_standard/eplusout.csv')
                except:
                    df_standard = None
                    
                draw_consumption(df_my_schedule, df_standard, input_name, 'electricity')
            else:
                window['-OUTPUT-'].update('You need to run the simulation to get the result.')
                
    window.close() 
    sys.exit(0)

if __name__ == '__main__':
    
    # sg.theme_previewer()
    
    # connect to firestore and handle errors
    try:
        cred = credentials.Certificate(f'{current_path}/config/myFirestoreKey.json')
        firebase_admin.initialize_app(cred)
        db = firestore.client()
        buildingList = []
        docs = db.collection(u'BuildingNameList').stream()
        for doc in docs:
            buildingList.append(doc.id)
    except:
        buildingList = ['The credential file for accessing firebase database is missing or Firebase is not responding or the internet connection fails.',]
    
    # buildingList = ['address1', 'address2', 'address3']
    
    main(tuple(buildingList))
