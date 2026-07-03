import re
with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = content.replace('        <attribution android:tag="safeme_sos_tag" android:label="@string/app_name" />\n', '')
content = content.replace('<application', '    <attribution android:tag="safeme_sos_tag" android:label="@string/app_name" />\n\n    <application')

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
print('Manifest updated again')
