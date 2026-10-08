#!/usr/bin/env python3
"""Create a self-contained offline HTML manual; Python stdlib only."""
import base64
import html
import mimetypes
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
TITLE = re.compile(r"^(#{1,6})\s+(.+)$")
ITEM = re.compile(r"^\s*(?:[-*]|\d+\.)\s+(.+)")
BOLD = re.compile(r"\*\*(.+?)\*\*")
IMAGE = re.compile(r"!\[([^\]]*)\]\(([^)]+)\)")
LINK = re.compile(r"\[([^\]]+)\]\(([^)]+)\)")

def inline(line, source):
    # Escape first, then replace only safe image/link targets.
    def img(m):
        target = (source.parent / html.unescape(m.group(2))).resolve()
        if not target.is_relative_to(ROOT) or not target.is_file():
            return "[Ілюстрація недоступна]"
        kind = mimetypes.guess_type(target.name)[0] or "application/octet-stream"
        payload = base64.b64encode(target.read_bytes()).decode("ascii")
        return '<img alt="' + m.group(1) + '" src="data:' + kind + ';base64,' + payload + '">'
    line = html.escape(line)
    line = IMAGE.sub(img, line)
    line = LINK.sub(lambda m: '<a href="' + m.group(2) + '">' + m.group(1) + '</a>' if m.group(2).startswith(("http", "#")) else m.group(1), line)
    return BOLD.sub(r"<strong>\1</strong>", line)

def render(source):
    rows = source.read_text(encoding="utf-8").splitlines()
    if rows and rows[0] == "---":
        for j in range(1, len(rows)):
            if rows[j] == "---":
                rows = rows[j+1:]
                break
    output, anchors = [], []
    i = 0
    while i < len(rows):
        s = rows[i].strip()
        if not s:
            i += 1
            continue
        h = TITLE.match(s)
        if h:
            level, name = len(h.group(1)), h.group(2)
            ident = "section-" + str(len(anchors))
            if level <= 3:
                anchors.append((ident, name))
            output.append(f'<h{level} id="{ident}">' + inline(name, source) + f'</h{level}>')
            i += 1
        elif s.startswith("|") and i+1 < len(rows) and re.fullmatch(r"[\s:\-|]+", rows[i+1]) and "-" in rows[i+1]:
            head = s.strip("|").split("|")
            items = ["<tr>" + "".join("<th>" + inline(x.strip(), source) + "</th>" for x in head) + "</tr>"]
            i += 2
            while i < len(rows) and rows[i].strip().startswith("|"):
                cells = rows[i].strip().strip("|").split("|")
                items.append("<tr>" + "".join("<td>" + inline(x.strip(), source) + "</td>" for x in cells) + "</tr>")
                i += 1
            output.append('<div class="table"><table>' + "".join(items) + "</table></div>")
        elif ITEM.match(s):
            numbered = s[0].isdigit()
            tag = "ol" if numbered else "ul"
            items = []
            while i < len(rows) and (found := ITEM.match(rows[i].strip())):
                if rows[i].strip()[0].isdigit() != numbered:
                    break
                items.append("<li>" + inline(found.group(1), source) + "</li>")
                i += 1
            output.append("<" + tag + ">" + "".join(items) + "</" + tag + ">")
        elif s.startswith(">"):
            quotes = []
            while i < len(rows) and rows[i].strip().startswith(">"):
                quotes.append(rows[i].strip().lstrip(">").strip())
                i += 1
            output.append("<blockquote>" + inline(" ".join(quotes), source) + "</blockquote>")
        elif s == "---":
            output.append("<hr>")
            i += 1
        else:
            para = [s]
            i += 1
            while i < len(rows) and rows[i].strip() and not TITLE.match(rows[i].strip()) and not ITEM.match(rows[i].strip()) and not rows[i].strip().startswith(("|", ">", "---")):
                para.append(rows[i].strip())
                i += 1
            output.append("<p>" + inline(" ".join(para), source) + "</p>")
    return "\n".join(output), anchors

STYLE = """
:root{color-scheme:dark}*{box-sizing:border-box}body{margin:0;background:#090f19;color:#eff6ff;font:16px/1.65 system-ui,sans-serif}
nav{position:fixed;left:0;top:0;bottom:0;width:268px;background:#112034;padding:20px 14px;overflow:auto}
nav a{display:block;color:#a7d4e6;font-size:13px;padding:5px 9px;text-decoration:none}
nav a:hover{background:#243e55}nav p{font-size:12px;color:#97b2c4}
main{max-width:1050px;margin-left:268px;padding:38px 32px 70px}
h1,h2,h3{line-height:1.25;scroll-margin-top:18px}h2{margin-top:36px;border-bottom:1px solid #345;padding-bottom:9px}
a{color:#64d8ec}code,pre{background:#1b3044}blockquote{border-left:3px solid #48cde7;background:#16273b;padding:10px 16px}
img{display:block;max-width:100%;max-height:400px;width:auto;height:auto;object-fit:contain;margin:16px auto;border-radius:10px}
.table{overflow:auto}table{border-collapse:collapse;width:100%;font-size:14px}td,th{border:1px solid #345;padding:9px;text-align:left}
th{background:#193951}li{margin:7px 0}input{width:100%;padding:9px;background:#102538;color:#fff;border:1px solid #54778c;border-radius:6px}
@media(max-width:850px){nav{position:relative;width:auto;max-height:250px}nav .links{max-height:115px;overflow:auto}main{margin:0;padding:22px 16px}img{max-height:300px}}
"""
def main():
    source = Path(sys.argv[1]).resolve()
    dest = Path(sys.argv[2])
    dest.parent.mkdir(parents=True, exist_ok=True)
    body, sections = render(source)
    links = "".join('<a href="#' + key + '">' + html.escape(name) + "</a>" for key, name in sections)
    page = ('<!doctype html><html lang="uk"><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">'
            '<title>FARIC Documentation</title><style>' + STYLE + '</style><nav><strong>FARIC / DOCUMENTATION</strong>'
            '<p>Офлайн-довідка. Зображення вбудовано.</p><input placeholder="Пошук розділу" oninput="for(let x of document.querySelectorAll(\'nav a\')) x.hidden=!x.textContent.toLowerCase().includes(this.value.toLowerCase())">'
            '<div class="links">' + links + '</div></nav><main>' + body + '</main></html>')
    dest.write_text(page, encoding="utf-8")
    print("Готова офлайн-довідка:", dest)

if __name__ == "__main__":
    main()
