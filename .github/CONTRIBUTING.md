# Catalogue requests

* Catalogue requests should be created at https://github.com/inorichi/tachiyomi-extensions/issues, not here

# Bugs
* Include version (Setting > About > Version)
 * If not latest, try updating, it may have already been solved
 * Dev version is equal to the number of commits as seen in the main page
* Include steps to reproduce (if not obvious from description)
* Include screenshot (if needed)
* If it could be device-dependent, try reproducing on another device (if possible), include results and device names, OS, modifications (root, Xposed)
* **Before reporting a new issue, take a look at the [FAQ](https://github.com/inorichi/tachiyomi/wiki/FAQ), the [changelog](https://github.com/inorichi/tachiyomi/releases) and the already opened [issues](https://github.com/inorichi/tachiyomi/issues).**
* For large logs use http://pastebin.com/ (or similar)
* For multipart issues **use list** like this:
 * [x] Done
 * [ ] Not done
```
* [x] Done
* [ ] Not done
```
* Don't put together too many unrelated requests into one issue

DO: https://github.com/inorichi/tachiyomi/issues/24 https://github.com/inorichi/tachiyomi/issues/71

DON'T: https://github.com/inorichi/tachiyomi/issues/75

# Feature requests

* Write a detailed issue, explaning what it should do or how. Avoid writing just "like X app does"
* Include screenshot (if needed)

# Translations

File `app/src/main/res/values/strings.xml` should be copied over to appropriate directories and then translated.
Consult [Android.com](http://developer.android.com/training/basics/supporting-devices/languages.html#CreateDirs)
