# playintegrityapi-rti

This repository stores the code for the application demonstrating how the Play Integrity API 
can be bypassed using the Root of Trust Identification Problem. This work is a real-world demonstration 
of an attack theorized by De Oliveira Nunes I, Ding X, Tsudik G. in their paper "On the Root of Trust Identification Problem".

The code consists of 4 parts:
- The legitimate Andriod application (PlayIntegrityAPITest). This application is uploaded to the Google Play Store and returns a valid verification on non-emulated devices.
- The legitimate application server. This is a server running on Google Cloud that coordinates between the legitimate application and the Google Play API to decrypt the API token.
- The Attacker Server. This is placed in between the connection between the legitimate application and the legitimate server to steal the token.
- The Attacker Application. This app is not uploaded to the Play Store and can run on an emulated device. This app reaches out to the attacker server to get the stolen token and then uses it as its token to forge a legitimate verification.

The legitimate app, legitimate server, and attacker app are all coded using Kotlin and run using Java/Andriod. 
The attacker server is coded in Python since it was easier to work with and I could directly pass data VIA raw sockets so nothing gets modified.

## Installation

To run this scenario, you will need a Google Cloud instance and a Google Play Console Developer account. 
Students get a period of free access to Google Cloud, but the Play Console account costs \$25 at the time of writing.

First, go to the Google Play Console and create an app. Fill out all the details that it requests. Google's guide to creating an app can be found here: https://support.google.com/googleplay/android-developer/answer/9859152

Next, go to the App Integrity section (on the left) and click "Link Cloud Project". Select "Create new project" and click "Link project". 
This will create a project associated with your Google account.

Third, go to Google Cloud Console and log in with your Google account. You should see that you have access to the Play API. 
Click the three bars on the left and select "Billing". Go through the process of setting up a billing account. Since I am a student, I was not charged for this project, but I still needed the account.

Now clone this repository onto a Linux machine (Windows might work but it's a lot harder for some reason). 
Before we can build this, make sure the Google Cloud SDK is installed (https://cloud.google.com/sdk/docs/install#deb). Make sure app-engine-java is installed.
Now go to the "server" directory from the cloned repo and run "./gradlew appengineDeploy". This should push the compiled server to Google Cloud. 
Make sure to find the URL for the server once the deployment is complete. Entering this URL in the browser should just return a simple Hello World. 
If it doesn't give you the URL after deployment, you can check it by going to "App Engine" in the Google Cloud Console and it should be in the top right of your dashboard.
For debugging purposes, logging for this server can be found in "Logging" > "Logs Explorer" in Google Cloud Console. 

Fifth, open up the PlayIntegrityAPITest folder in Andriod Studio. Click "Build" > "Generate Signed Bundle". Select "Android App Bundle" and click next. 
It may ask you to set up a keystore to sign the bundle. Make the password something memorable as you will need it every time you upload the file to Google Play Console. 
Finish creating the bundle and find the output. Go back to the Google Play Console and create a release. I only released it in the "Internal Testing" branch
so that the file wasn't public. Upload the signed package to the release and fill out any other information required. If using internal releases, 
make sure your email is on the distribution list so that your phone gets updates to this app. 

Sixth, set up a BIND9 DNS server on your local network. Create a zone for appspot.com. This is where your application server is normally hosted. 
Create an A record in this zone rerouting the full domain name of your Application Server to the IP address of the Python attacker server. 
Run the BIND9 server and use NSLookup to verify that the IP of your application server URL has changed to the attacker's Python server.

Next, go to the AttackerServer folder and run the Python script. There shouldn't be any extra libraries to install for this script so it should just run. 

Finally, open the AttackerApp folder in Andriod Studio, and update the URL on line 54 to the location of your Python attacker server. 

## Running the scenario

Legitimate scenario:

The legitimate server should already be running from installation, but verify this by going to your Google Cloud Project URL and seeing if it displays "Hello World".

Next, download the application you created in the Google Play Console onto your phone. When you open this application, click "Verify Now" and you should get a result
saying that your phone is legitimate. Note: if your phone is rooted, or using a non-standard OS, this verification will fail.

Attacker Scenario:

First, ensure the Python server is running. Then go to your router settings and update the DNS server to your local BIND9 server. This will allow the connection to be intercepted.
Click "Verify Now" on your legitimate device. This verification should fail, but you will see text on the Python server indicating it intercepted the connection.
Then, run the attacker application inside Andriod Studio and click "Verify Now". This will use the token provided by the legitimate device and pass verification.

A demonstration of this attack can be found at https://youtu.be/oMpcEH0dLO8