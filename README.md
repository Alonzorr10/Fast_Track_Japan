# Fast Track Japan

## Team Members

Alonzo Rico and Ken Tze Cheng

## Overview

Fast Track Japan is a Mobile Application created for foreigners living in Japan or anyone living in Japan for the first time. It serves to address some of the common issues
and problems these users tend to face. Tools such as a Bill recorder, an Expiration Date reminder, and just general resources available at the click of a button. The application
serves to simplify as many of these issues into one application and hopefully encourage more individuals to move to Japan and not be intimidated by all the processes and procedures

## Tech Stack
- Kotlin (Android Studio)
- Supabase (PostgreSQL)

## Prerequisites
- Git
- Android Studio

## Git
<a href="https://git-scm.com/install/windows">Website</a>

For git, if you are on Windows,
- Make sure to set the Bin path in git as one of your environment variables.
- To do this, navigate to your Program Files and locate Git.
- Then, look for the bin file and copy the path.
- Afterwards, in your Windows Serach Bar, search for Environment Variables.
-  Once open, select Environment Variables. At the top, you will see lots of paths, look for "Path" and double click.
-  More paths will appear, from here click "New" and you will be prompted with a new path.
-  Paste the path to the Bin and save your changes

## Installation FLOW

- Ensure Android Studio is open as well, if not downloaded follow the link
  <a href = "https://developer.android.com/studio">Website</a>  
- Navigate to the green “Code” button at the top right and click
- Find “HTTPS” and copy the link given
- Now, open Android studio and you’ll be presented with the “Welcome Screen” to create a new project
- Instead of doing that, navigate to the “Get from VCS” button
- After you find and click this button, you’ll be prompted to paste a URL, use the git link you copied from the GitHub project link and paste
- Click clone and you should be taken a new window with all the code in place. 
- Before hitting run, navigate to “running devices” on the right side panel.
- Click the “+” at the top of this window and select a device (I am using Small Phone API 37.0)
- After selecting your device and ensuring the emulator is running, click the play button at the top to run the application. 
- After a bit of time, the application should start running and will be fully usable

## File Structure Overview (brief summary over important files)
- AuthScreens.kt: Kotlin file responsible for handling the Login and Sign Up screens. Directly interacts with SupabaseClient.kt to make calls to our Supabase
- BillCaptureComponent.kt, BillData.kt, BillLibraryScreen.kt, CameeraCaptureScreen.kt, BillViewModel: These are the files that handle the Bill Capture functionality.
- **BillCaptureComponent** is the initial screen when clicking the component on the Main Menu and offers the user two options: Screenshot a Bill or View Library
- **CameraCaptureScreen** is the camera screen that allows the user to take a picture of the bill.
- **BillData** is simply the class file that denotes the attributes of a bill. We use @Serializable to allow for "bills" to be used when refering to a bill instance
- **BillViewModel** is where all the functions pertaining to a bill are housed and are called in the respective Bill related files
- **BillLibraryScreen** is the library screen that shows all the captured bills with their corresponding labels and dates
- ExpirationTrackerScreen.kt, EditBillScreen.kt, DocumentData.kt, DocumentViewModel: These are the files that handle the Expiration Date tracking component
- **ExpirationTrackerScreen** is the initial screen when clicking the component on the Main Menu and shows the user any existing documents that have already been saved as well as a button to add a new document
- **EditBillScreen** is the screen when a user wants to edit the details of a certain document and is triggered when clicking on a document
- **DocumentData** is simply the class file that denotes the attributes of a document. We use @Serializable to allow for "ExpirationDocument" to be used when refering to a document instance
- **DocumentViewModel** is where all the functions pertaining to a document are housed and are called in the respective Document related files
- ResourceCenterScreen.kt: This is the component that simply holds important resources for a user to access and download
- ProfileScreen.kt, ProfileViewModel, UserData.kt: These are the files that have to do with the user's profile information
- **ProfileScreen** is the actual screen where a user can view and edit their information
- **UserData** is the class file that denotes the attributes of a user. We use @Serializable to allow for "UserProfile" to be used when refering to a user instance
- **ProfileViewModel** is where all the functions pertaining to a user are housed
- MainMenuScreen.kt: This is the main screen users are redirected to on log in or sign up and holds all the components to be navigated into
- MainActivity.kt: The **true** starting screen and is what users will see when they open the app
- SupabaseClient.kt: This is where we create our client instance that directly connects with our Supabase. We will use this "client" instance in AuthScreen.kt and other files that directly interact with Supabase
- DateField.kt: This is a file that simply transforms any date fields whether when saving when a Bill is taken or when a document expires and utilized an easier to use component

## Attribution
- All Kotlin files are generated by Gemini 3.5 Flash and Claude
- Any functions, classes, or variables are handled by both of us and are adjusted accordingly when the application calls for changes
- The SupabaseClient.kt file was made using the Supabase Documentation <a href = "https://supabase.com/docs/guides/auth">Supabase</a> 
 

