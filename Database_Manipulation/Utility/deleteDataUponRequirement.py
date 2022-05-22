import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import schedule
import time

# function for deleting data
def delete_collection(coll_ref, batch_size):
    docs = coll_ref.limit(batch_size).stream()
    deleted = 0

    for doc in docs:
        doc.reference.delete()
        deleted = deleted + 1

    if deleted >= batch_size:
        return delete_collection(coll_ref, batch_size)
    
def main():
    
    # connect to firestore
    cred = credentials.Certificate("./myFirestoreKey.json")
    firebase_admin.initialize_app(cred)
    db = firestore.client()

    # check every user for deletion requirement
    docs = db.collection(u'RegisteredUser').stream()
    for doc in docs:
        deleteRequirement = doc.to_dict()['deleteRequirement']
        if deleteRequirement == 'YES':
            userName = doc.id
            try:
                # if the user has data collected, delete them as well
                targetBuilding = doc.to_dict()['targetBuilding']
                delete_collection(db.collection(targetBuilding).document(userName).collection(u'POSITIONING'), 10)
                delete_collection(db.collection(targetBuilding).document(userName).collection(u'GEOFENCE'), 10)
                delete_collection(db.collection(targetBuilding).document(userName).collection(u'MANUAL'), 10)
                delete_collection(db.collection(targetBuilding).document(userName).collection(u'WIFI'), 10)
                db.collection(targetBuilding).document(userName).delete()
                db.collection(u'RegisteredUser').document(userName).delete()
            except:
                # otherwise only delete personal data
                db.collection(u'RegisteredUser').document(userName).delete()


if __name__=="__main__":
    
    # perform the checking and deletion everyday at 1am
    schedule.every().day.at("01:00").do(main)

    while True:
        schedule.run_pending()
        time.sleep(60*24) 
        
    # main()