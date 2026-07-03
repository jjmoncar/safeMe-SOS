import re
with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

queries = '''
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
    <queries>
        <intent>
            <action android:name="android.speech.RecognitionService" />
        </intent>
    </queries>

    <application'''

content = content.replace('    <application', queries)

attribution = '''        android:theme="@style/Theme.MyApplication">

        <attribution android:tag="safeme_sos_tag" android:label="@string/app_name" />'''

content = content.replace('        android:theme="@style/Theme.MyApplication">', attribution)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
print('Manifest updated')
