import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

text = re.sub(r'CoroutineScope\(Dispatchers\.IO\)\.launch \{\s*MobileAds\.initialize\(this@MainActivity\) \{\}\s*\}', 'MobileAds.initialize(this) {', text)
text = re.sub(r'AdManager\.loadInterstitialAd\(this\)', 'AdManager.loadInterstitialAd(this)\n    }', text)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
