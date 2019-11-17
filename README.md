# NotifyWho
Custom Notifications Tones &amp; Vibration Patterns for WhatsApp.

## Functionality
Lets users select contacts and assign to them indivudally different ringtones and vibration patterns. Also allows for the creation of new vibration patterns with a vibration generator activity. 

<p float="left">
<img src="/Published/Playstore/Screenshot/1_one.png?raw=true" width=270/>
<img src="/Published/Playstore/Screenshot/2_two.png?raw=true" width=270/>
<img src="/Published/Playstore/Screenshot/3_three.png?raw=true" width=270/>
<img src="/Published/Playstore/Screenshot/4_four.png?raw=true" width=270/>
<img src="/Published/Playstore/Screenshot/5_five.png?raw=true" width=270/>
<img src="/Published/Playstore/Screenshot/6_six.png?raw=true" width=270/>
</p>

## How it works
By using Android's  ```NotificationListenerService```, NotifyWho intercepts WhatsApp notifcations, checks incoming text message sender names against a list of custom contacts, and then vibrates / plays the sound given to that contact. 

## Implemenation 
The app is divided into three main activities: 
- ```PermissionsActivity``` : The 'Welcome' activity with a series of Fragments that explain how NotifyWho works and ask the user for permissions
- ```MainActivity``` : A three fragment view consisting of "Contacts", "Default", and "Help" sections. 
  - "Contacts" : The fragment in which currently defined contacts with specific ringtone / vibration patterns are displayed and can be edited
  - "Default" : The fragment in which the default notification sounds / vibrations can be set 
  - "Help" : The fragment where users can read a help section, rate the app, and ask for developer help
- ```VibrationPickerActivity``` : An activity that mimics Android's Ringtone Picker activity, the difference being that users select from a list of pre-defined and custom vibrations. Users can add custom vibrations by clicking a plus button. By tapping on the screen rythmically, new vibrations are generated and saved permanently. 

## General Project Structure 

  - app/src/main/java/com.tzgames.ringer/
    - activities 
      - PermissionsActivity
      - MainActivity
      - VibrationPickerActivity
    - fragments
      - intro (for PermissionsActivity)
        - ...
      - main (for MainActivity)
        - ...
      - vibration (for VibrationPickerActivity)
        - ...
    - services (for NotificationListenerService)
      - ...
    - data (for saving / loading / getting permanent user data) 
      - ...
    - views (custom views used by fragments)
      - ...

## On Google Play
Link to Google Play Store page: [link](https://github.com/travisjayday/NotifyWho/tree/master/app/src/main/java/com/tzgames/ringer)

## Contact
The developer / maintainer is <travisjayday@gmail.com>
