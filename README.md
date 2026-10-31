# Auto YT Uploader

An Android app that uploads every video in one album you choose to your YouTube channel, as a
**private** video, then moves it into an album called **Uploaded** on your phone.

This guide takes you from nothing to a working upload. No coding is needed. Do the parts in
order and don't skip any. Allow about 30 minutes.

## Contents

- [Before you start](#before-you-start)
- [Part 1: Set up Google Cloud](#part-1-set-up-google-cloud)
- [Part 2: Install the app](#part-2-install-the-app)
- [Part 3: Set up the app](#part-3-set-up-the-app)
- [Part 4: Test it](#part-4-test-it)
- [Everyday use](#everyday-use)
- [Good to know](#good-to-know)
- [Troubleshooting](#troubleshooting)
- [For developers: rebuilding the app](#for-developers-rebuilding-the-app)

---

## Before you start

You need:

- An Android phone running **Android 11 or newer**. To check, open **Settings > About phone**
  and look for **Android version**.
- The **Google account that owns your YouTube channel**. If you don't have a channel yet, sign
  in at [youtube.com](https://www.youtube.com) and create one first.
- The app file, `app-release.apk`. In this project it is in `app/build/outputs/apk/release/`.
- A computer with a web browser for Part 1.

Keep these two values handy. You will paste them in Part 1, Step 5.

| Field | Value |
| --- | --- |
| Package name | `com.autoyt.uploader` |
| SHA-1 fingerprint | `42:9B:12:22:9A:B6:4A:AC:9B:2E:E3:C0:8E:49:79:F8:F5:BB:AB:04` |

> **Use exactly this SHA-1.** It identifies the key that signed this APK. Other guides tell you
> to run `gradlew signingReport` or read `debug.keystore`. Don't: those give a different value
> and sign-in will fail. If you build the app with your own key, use your key's SHA-1 instead
> (see [For developers](#for-developers-rebuilding-the-app)).

---

## Part 1: Set up Google Cloud

This gives the app permission to talk to YouTube. It's free, and you only do it once.

Google changes these screens from time to time. If a button has moved, look for the same word
nearby.

### Step 1: Create a project

1. Go to [console.cloud.google.com](https://console.cloud.google.com/) and sign in with the
   **account that owns your YouTube channel**.
2. If this is your first visit, accept the terms.
3. Click the **project selector** at the top of the page, then **New Project**.
4. For **Project name**, enter `AutoYTUploader`.
5. Click **Create** and wait a few seconds.
6. Open the project selector again and choose **AutoYTUploader**.

**Check:** `AutoYTUploader` now shows at the top of the page. Make sure it stays selected for
every step below.

### Step 2: Turn on the YouTube API

1. Open the menu (☰) at the top left, then **APIs & Services > Library**.
2. Search for `YouTube Data API v3`.
3. Click the result, then click **Enable**.

**Check:** the page now shows **Manage** or **API Enabled**.

### Step 3: Create the consent screen

This is the permission screen you'll see when you sign in from the app.

1. Open the menu (☰), then **Google Auth Platform**. If you don't see it, use
   **APIs & Services > OAuth consent screen**.
2. Click **Get started**.
3. Under **App Information**, enter:
   - **App name:** `Auto YT Uploader`
   - **User support email:** your email address
4. Click **Next**.
5. Under **Audience**, choose **External**, then click **Next**.
6. Under **Contact Information**, enter your email address, then click **Next**.
7. Agree to the policy, click **Continue**, then click **Create**.

### Step 3b: Put a homepage and privacy policy online

Google won't let you publish the app without these two public web pages. They're ready in the
project's `site` folder and can be hosted free on GitHub Pages.

1. Sign in or sign up at [github.com](https://github.com).
2. Create a [new repository](https://github.com/new) named `autoyt-uploader-site`. Set it to
   **Public** and click **Create repository**.
3. Click **uploading an existing file**. Drag in `index.html` and `privacy.html` from the `site`
   folder (the two files, not the folder), then click **Commit changes**.
4. Open the repository's **Settings > Pages**.
5. Under **Branch**, choose **main**, keep **/ (root)**, then click **Save**.
6. Wait a minute or two, then open these, using your GitHub username:
   - `https://YOUR_USERNAME.github.io/autoyt-uploader-site/`
   - `https://YOUR_USERNAME.github.io/autoyt-uploader-site/privacy.html`
7. In **Google Auth Platform > Branding**, enter:
   - **Application home page:** the first address
   - **Application privacy policy link:** the second address
   - **Authorized domains:** click **Add domain** and enter `YOUR_USERNAME.github.io`
8. Click **Save**.

**Check:** both addresses open in a private browser window, and **Branding** saves without errors.

### Step 4: Add the upload permission and publish

1. In **Google Auth Platform**, click **Data Access** in the left menu.
2. Click **Add or remove scopes**.
3. In the filter box, type `youtube.upload`.
4. Tick the row for `https://www.googleapis.com/auth/youtube.upload`.
5. Click **Update**, then **Save** at the bottom of the page.
6. Click **Audience** in the left menu.
7. Under **Publishing status**, click **Publish app**, then **Confirm**.

**Check:** **Publishing status** now says **In production**.

> **Don't skip the publish step.** If the status stays on **Testing**, your sign-in expires
> every 7 days and uploads stop. You do **not** need to submit the app for verification, even if
> Google suggests it.

### Step 5: Create the Android client

1. In **Google Auth Platform**, click **Clients** in the left menu. (You can also use
   **APIs & Services > Credentials**, then **Create Credentials > OAuth client ID**.)
2. Click **Create client**.
3. For **Application type**, choose **Android**.
4. Fill in:
   - **Name:** `Auto YT Android Client`
   - **Package name:** `com.autoyt.uploader`
   - **SHA-1 certificate fingerprint:**
     `42:9B:12:22:9A:B6:4A:AC:9B:2E:E3:C0:8E:49:79:F8:F5:BB:AB:04`
5. Click **Create**.

**Check:** `Auto YT Android Client` appears in your list of clients.

You're finished with Google Cloud. You don't need to copy anything from this page into the app,
and you do **not** need a Web client, a client secret or an API key.

Changes can take a few minutes to reach Google's servers, so wait about 5 minutes before Part 3.

---

## Part 2: Install the app

If an older copy of this app is on your phone, **uninstall it first**. Android refuses to update
an app signed with a different key.

### Option A: Copy the file to your phone (easiest)

1. Send `app-release.apk` to your phone by USB cable, Google Drive or similar.
2. On the phone, open **Files** and tap `app-release.apk`.
3. If asked, tap **Settings**, turn on **Allow from this source**, then go back.
4. Tap **Install**.
5. If Google Play Protect warns about an unknown app, tap **More details > Install anyway**.

### Option B: Install over USB

1. On the phone, open **Settings > About phone** and tap **Build number** seven times.
2. Open **Settings > Developer options** and turn on **USB debugging**.
3. Connect the phone to your Windows PC and tap **Allow** on the phone.
4. Open Command Prompt in the project folder and run:

   ```cmd
   "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb" install app\build\outputs\apk\release\app-release.apk
   ```

**Check:** the output says `Success`, or **Auto YT Uploader** appears in your apps.

---

## Part 3: Set up the app

Open **Auto YT Uploader**. When it asks to send notifications, tap **Allow**. Then, in this order:

### Step 1: Sign in to YouTube

1. Tap **Sign in to YouTube**.
2. Choose the **account that owns your YouTube channel**.
3. If you see **Google hasn't verified this app**, tap **Advanced**, then **Go to Auto YT
   Uploader**. This warning is expected, because you made this app yourself.
4. Allow the permission to manage your YouTube videos, then tap **Continue**.

**Check:** the screen says **Signed in to YouTube**.

### Step 2: Allow access to videos

1. Tap **Allow access to videos**.
2. Turn on the switch for **Auto YT Uploader**. It may be called **Allow access to manage all
   files**.
3. Go back to the app.

**Check:** the button disappears within a few seconds.

### Step 3: Allow running in the background

1. Tap **Allow running in the background**.
2. Tap **Allow**.

**Check:** the button disappears within a few seconds.

Some phones, including Samsung, Xiaomi and Huawei, stop background apps even after this. If yours
does, see [Uploads only happen when the app is open](#uploads-only-happen-when-the-app-is-open).

### Step 4: Choose your album

> **Everything in this album gets uploaded, including videos already there.** Check it first.
> If you pick your main camera album, every video you record will upload and then move out of it.

If you want to pick which videos upload, first create a folder for it, such as `Movies/ToYouTube`,
with your **Files** app and put at least one video in it. Empty folders don't appear in the list.

1. Tap **Choose album**.
2. Tap the album. Uploading starts right away.

**Check:** the screen shows **Album to upload from:** and your album's name.

### Step 5: Set the mobile data limit

**Largest video on mobile data (MB)** starts at `100`. Videos up to this size upload on any
connection. Larger ones wait for Wi-Fi. As a guide, one minute of 4K video is about 400 MB.

To never use mobile data, enter `0`.

---

## Part 4: Test it

1. Connect to Wi-Fi.
2. Put a **short** video in your chosen album.
3. Wait about a minute. New videos must sit unchanged for 30 seconds before they upload.
4. Open the app and look under **Recent uploads**. The video should show **done**.
5. Open [studio.youtube.com](https://studio.youtube.com) and click **Content**. The video should
   be listed as **Private**, titled with its filename.
6. In your phone's gallery, check that the video is now in the **Uploaded** album.

If all of that worked, setup is complete. Otherwise, see [Troubleshooting](#troubleshooting).

---

## Everyday use

Move or save a video into your chosen album. The app does the rest, with no need to open it.

- **Timing:** usually within a minute or so, with a backup check about every 15 minutes.
- **Interruptions:** if the connection drops, the upload continues from where it stopped.
- **Status:** under **Recent uploads**, each video shows **uploading**, **done** or **failed**.
- **Upload now and retry failures:** starts a check immediately, retries failed videos and
  clears a daily-limit pause.

---

## Good to know

- **Videos stay private, and can't be published yet.** Google restricts uploads from unaudited
  API projects like yours to private. They can't be made public or unlisted until the project
  passes a [YouTube API audit](https://support.google.com/youtube/contact/yt_api_form).
  Your own viewing works normally.
- **Daily limit:** YouTube's default limit is about 100 uploads a day. If you reach it, the app
  notifies you and carries on after midnight Pacific time.
- **Originals stay on your phone.** They move to `Movies/Uploaded` and are never deleted. YouTube
  doesn't keep a copy at your original quality, so keep any you care about.
- **Made for Kids:** the app doesn't set this. YouTube Studio may ask you to choose.
- **Uninstalling** erases the app's upload history. Videos still in the album after you reinstall
  could upload a second time.
- **Stopping access:** there's no sign-out button. Uninstall the app, or remove it at
  [myaccount.google.com/permissions](https://myaccount.google.com/permissions).

---

## Troubleshooting

### "Sign-in failed" (often with the number 10)

Google doesn't recognise the app. In Google Cloud, open your Android client and check:

- The package name is exactly `com.autoyt.uploader`.
- The SHA-1 is exactly the one in [Before you start](#before-you-start).
- The client is in the same project as the enabled YouTube API.

After a change, wait 5 minutes and try again.

### "The Android client can't be created because the fingerprint is in use"

The same package name and SHA-1 are already registered in another Google Cloud project, usually
one you made earlier. Delete the client there, or use that project.

### "Access blocked: has not completed the Google verification process" (Error 403: access_denied)

The app is still in **Testing** and your account isn't an approved tester. In the project in
Google Cloud, open **Google Auth Platform > Audience**, click **Publish app**, then **Confirm**.
**Publishing status** should say **In production**. You don't need to submit for verification.
Wait 5 minutes, then sign in again. This time the screen should offer **Advanced**.

### "Access blocked" or "This app is blocked" at sign-in

You may be using a different account from the one that created the project, or the consent screen
may be unfinished. Recheck Part 1, Steps 3 and 4.

### The app asks you to sign in again every week

**Publishing status** is still **Testing**. Follow Part 1, Step 4, then sign in again.

### Your album isn't in the list

The list only shows folders containing at least one video, and it hides the **Uploaded** album.
Add a video, check that **Allow access to videos** was granted, then tap **Choose album** again.

### Nothing uploads

Go through this list:

1. The screen says **Signed in to YouTube**.
2. The **Allow access to videos** button is gone.
3. An album is selected.
4. The video is in that album, and not one that already shows **done** or **failed**.
5. You're on Wi-Fi, or the video is under your mobile data limit.
6. You've waited at least a minute. Then tap **Upload now and retry failures**.

### Uploads only happen when the app is open

Your phone is stopping the app in the background. Open **Settings > Apps > Auto YT Uploader >
Battery**, and choose **Unrestricted**. Names vary by phone. Also turn off any **Battery saver**.

[dontkillmyapp.com](https://dontkillmyapp.com) has steps for each phone brand.

### A video shows "failed"

Read the message beside it. `YouTube returned 403` with `youtubeSignupRequired` means the account
has no YouTube channel yet: create one, then tap **Upload now and retry failures**.

### "Uploaded, but could not be moved to the Uploaded album"

The upload worked and won't repeat. Check that **Allow access to videos** is still on, then move
the video by hand if you want to.

### A notification says the daily limit was reached

This is normal. Uploads continue after midnight Pacific time.

### "App not installed"

An older copy signed with a different key is on the phone. Uninstall it and install again.

---

## For developers: rebuilding the app

Skip this unless you're changing the code or making your own copy.

**Share the APK only with care.** One signing key belongs to one Google Cloud project. Anyone else
setting this up should build it with their own key, as described here, and use that key's SHA-1.

### Requirements

- JDK 17
- Android SDK with `platforms;android-35`, plus `build-tools;35.0.0`
- `local.properties` in the project root, using forward slashes:

  ```properties
  sdk.dir=C:/Users/YOUR_NAME/AppData/Local/Android/Sdk
  ```

The app is native Kotlin, using Jetpack Compose and WorkManager.

### Create a signing key

Skip this if `autoyt.jks` already exists. Run these from the project root, replacing
`YOUR_PASSWORD` in both places:

```cmd
keytool -genkeypair -keystore autoyt.jks -storepass YOUR_PASSWORD -keypass YOUR_PASSWORD -alias autoyt -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Auto YT Uploader"
```

Then create `keystore.properties` in the project root containing one line:

```properties
password=YOUR_PASSWORD
```

### Get the SHA-1

```cmd
keytool -list -v -keystore autoyt.jks -alias autoyt
```

Enter your password and copy the value after `SHA1:`.

### Build and test

```cmd
gradlew testReleaseUnitTest assembleRelease
```

The APK is written to `app/build/outputs/apk/release/app-release.apk`.

### Protect the key

Back up `autoyt.jks` and `keystore.properties` somewhere safe, and never commit or share them.
Both are in `.gitignore`. Losing them means creating a new key, updating the SHA-1 in Google
Cloud and reinstalling the app. `old-signing-key/` holds a previous key and isn't used by builds.
