from pathlib import Path
import subprocess,sys,zipfile,shutil,os
root=Path(__file__).resolve().parent
server=root/'server'
cmd=[sys.executable,'-m','pytest','-q','test_schedule_rules.py','test_v17_contract.py','test_v18_event_order.py','test_v19_effective_counts.py','test_v20_event_projection.py','test_v22_conflicts.py','test_v23_resolution.py','test_v24_review_ui.py']
p=subprocess.run(cmd,cwd=server)
if p.returncode:sys.exit(p.returncode)
for f in server.glob('*.py'):
    compile(f.read_bytes(),str(f),'exec')
print('PASS Python syntax and isolated test suite')
print('Android SDK:',bool(os.getenv('ANDROID_HOME') or os.getenv('ANDROID_SDK_ROOT') or shutil.which('sdkmanager')))
print('Gradle:',bool(shutil.which('gradle')))
print('WARNING: APK build, TLS, device recovery, and end-to-end school server testing NOT completed')
