#!/bin/bash

# Git commands to commit the multi-language support feature

echo "Adding all files to git..."
git add .

echo "Committing changes..."
git commit -m "feat: Add comprehensive multi-language support

- Add support for 7 languages: Simplified Chinese (default), English, Traditional Chinese, Japanese, Russian, French, and German
- Implement automatic language detection based on system locale with region-specific handling for Chinese variants
- Add manual language selection in settings with intuitive UI
- Create LocaleHelper utility class for language management
- Add NotifyForwardersApplication class for app-wide locale configuration
- Implement persistent language preference storage
- Add language selection dialog with radio button interface
- Support immediate language switching with app restart
- Ensure all user-facing strings are internationalized
- Update README documentation with multi-language information

Files added:
- app/src/main/res/values-en/strings.xml (English)
- app/src/main/res/values-zh-rTW/strings.xml (Traditional Chinese)
- app/src/main/res/values-ja/strings.xml (Japanese)
- app/src/main/res/values-ru/strings.xml (Russian)
- app/src/main/res/values-fr/strings.xml (French)
- app/src/main/res/values-de/strings.xml (German)
- app/src/main/java/com/hestudio/notifyforwarders/util/LocaleHelper.kt
- app/src/main/java/com/hestudio/notifyforwarders/NotifyForwardersApplication.kt

Files modified:
- app/src/main/res/values/strings.xml (added language settings strings)
- app/src/main/java/com/hestudio/notifyforwarders/util/ServerPreferences.kt (added language preference storage)
- app/src/main/java/com/hestudio/notifyforwarders/SettingsActivity.kt (added language selection UI)
- app/src/main/java/com/hestudio/notifyforwarders/MainActivity.kt (added locale context wrapping)
- app/src/main/AndroidManifest.xml (registered Application class)
- README.md (updated with multi-language information)
- README_CN.md (updated with multi-language information)

Breaking changes: None
Backward compatibility: Maintained - existing users will see default language based on system settings"

echo "Commit completed!"
echo ""
echo "To push to remote repository, run:"
echo "git push origin [your-branch-name]"
