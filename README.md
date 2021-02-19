# TakeHomeChallenge2021

Supporting API 21 and above

Since the project design document specified a button press color, I went with that and intentionally
overrode the default behavior of the current Material Button - this also removes the ripple effect.

Since the background graphic was not compatible with landscape mode, I locked the app to portrait mode.

I had not previously used the new(ish) CameraX library so I integrated it at first, then realized it didn't
offer as much functionality of launching the native camera app via an intent - both implementations can be
accessed via a switch in PlayFragment.

Since you supplied iOS-sized bitmap assets, I just placed the highest resolution version in the drawable-xxhdpi
folder and didn't bother resizing to populate the various density folders - although I did let Android Studio
process the launcher icon.

I integrated the Karla font as an embedded typeface as it didn't seem to be the appropriate time to delve into
the newer functionality of Variable type faces.

I adjusted the aspect of the tiles slightly so that the 4-up grid would nicely fill the screens of both devices
I tested with - Samsung Note 4 and Samsung Galaxy Tab A. I made the horizontal margins and spacing of the grid a
ratio of the screen width, and then ensured that the vertical spacing matched the horizontal.

Although this was a simple app with just two screens, I still opted to use my preferred architecture of a single
activity hosting the two fragments with all navigation (and parameters when necessary) supplied in a nav graph.

To simplify camera permission handling, I used a third party library I am comfortable with and handled the
request and enforcement within the main activity, with a simple fatal error alert if the user does not grant.

I used a simple coroutine implementation to wrap the asynchronous API calls. Using a ViewModel would have been an
option but I wanted to heed the directions that suggested the welcome screen should acquire the data rather than
the play screen itself. As such, I simply kick the user back to the welcome screen to restart the game each time.

Things I might do if spending more time on the app:
- add a recycler view, adapter, and view holder class for the tiles - although it's probably overkill since all 4 tiles fit

- integrate the support library and utilize the new font embedding available with API 26+. Then the Karla typeface
could be specified in XML instead of in the UI code.

- add a mechanism for reporting (and retrying) internet connectivity issues - as it, there is coroutine exception
handler that simply reports all errors as fatal alert dialogs
