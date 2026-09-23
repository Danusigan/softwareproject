import sys,json,pathlib
sys.path.insert(0,'.codex-tmp/pdf-tools')
from playwright.sync_api import sync_playwright
root=pathlib.Path('Software-project-Backend/target/report-samples')
report=json.loads((root/'academic-progress-sample.json').read_text())
with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='C:/Users/thiru/AppData/Local/ms-playwright/chromium-1234/chrome-win64/chrome.exe',headless=True)
    page=browser.new_page(viewport={'width':1440,'height':1000})
    errors=[]
    page.on('pageerror',lambda e:errors.append(str(e)))
    page.add_init_script("localStorage.setItem('token','sample-browser-token');localStorage.setItem('isLoggedIn','true');localStorage.setItem('tokenExpiry',String(Date.now()+3600000));localStorage.setItem('userType','lecture');localStorage.setItem('username','teacher');")
    def route(r):
        if '/students?' in r.request.url:
            r.fulfill(json=[{'student_id':'S1','student_name':'Sample Student','batch':'22'}])
        elif r.request.url.endswith('/pdf'):
            r.fulfill(content_type='application/pdf',body=(root/'academic-progress-sample.pdf').read_bytes())
        else:r.fulfill(json=report)
    page.route('**/api/reports/progress/**',route)
    page.goto('http://127.0.0.1:5175/student-reports')
    page.get_by_label('Student index or name').fill('Sample')
    page.get_by_role('button',name='Find students',exact=True).click()
    page.get_by_label('Student',exact=True).select_option('S1')
    page.get_by_role('button',name='Generate and preview').click()
    page.get_by_role('button',name='Download full PDF').wait_for()
    page.screenshot(path=str(root/'progress-desktop.png'),full_page=True)
    page.get_by_text('Calculation and evidence for PO1',exact=True).click()
    assert page.get_by_text('SE101 / LO1',exact=True).is_visible()
    with page.expect_download() as downloaded:
        page.get_by_role('button',name='Download full PDF').click()
    assert downloaded.value.suggested_filename.endswith('.pdf')
    page.set_viewport_size({'width':390,'height':844})
    page.screenshot(path=str(root/'progress-mobile.png'),full_page=True)
    width=page.evaluate('({body:document.documentElement.scrollWidth,viewport:innerWidth})')
    assert width['body']<=width['viewport'],width
    assert not errors,errors
    print('Browser checks passed: desktop/mobile preview, evidence expansion, snapshot PDF download, no page overflow or JS errors.')
    browser.close()
