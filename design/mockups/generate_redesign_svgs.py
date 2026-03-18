from pathlib import Path
from html import escape

OUT = Path('design/mockups')
OUT.mkdir(parents=True, exist_ok=True)

P = {
    'bg': '#FFF8F3',
    'paper': '#FFFDF9',
    'card': '#FFFDFC',
    'ink': '#5D514C',
    'muted': '#8B7C75',
    'line': '#E8DCCF',
    'pink': '#F7CAD6',
    'pink_dark': '#D88FA7',
    'mint': '#DCEAD9',
    'mint_dark': '#93B29A',
    'butter': '#F8E6B8',
    'butter_dark': '#C9A75C',
    'sky': '#DDE9F7',
    'sky_dark': '#8AA4C4',
    'night': '#222024',
}

HEAD = "'SF Pro Display', 'PingFang SC', sans-serif"
BODY = "'SF Pro Text', 'PingFang SC', sans-serif"


def svg_doc(w, h, content):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">
<style>
.h1 {{ font: 700 54px {HEAD}; fill: {P['ink']}; }}
.h2 {{ font: 700 32px {HEAD}; fill: {P['ink']}; }}
.h3 {{ font: 700 22px {HEAD}; fill: {P['ink']}; }}
.body {{ font: 500 20px {BODY}; fill: {P['muted']}; }}
.small {{ font: 500 16px {BODY}; fill: {P['muted']}; }}
.label {{ font: 700 14px {BODY}; letter-spacing: 2px; }}
.nav {{ font: 600 16px {BODY}; }}
</style>
{content}
</svg>'''


def text(x, y, value, cls='body', fill=None, anchor='start'):
    fa = f' fill="{fill}"' if fill else ''
    return f'<text x="{x}" y="{y}" class="{cls}" text-anchor="{anchor}"{fa}>{escape(value)}</text>'


def rect(x, y, w, h, fill, rx=24, stroke=None, sw=2, opacity=None):
    sa = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ''
    oa = f' opacity="{opacity}"' if opacity is not None else ''
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}"{sa}{oa}/>'


def circle(cx, cy, r, fill, stroke=None, sw=2, opacity=None):
    sa = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ''
    oa = f' opacity="{opacity}"' if opacity is not None else ''
    return f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{fill}"{sa}{oa}/>'


def line(x1, y1, x2, y2, stroke, sw=3):
    return f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{stroke}" stroke-width="{sw}" stroke-linecap="round"/>'


def flower(cx, cy, petal='#F7CAD6', center='#F8E6B8', scale=1.0):
    p = []
    for dx, dy in [(0,-18), (16,-4), (10,14), (-10,14), (-16,-4)]:
        p.append(circle(cx + dx*scale, cy + dy*scale, 14*scale, petal, stroke=P['line'], sw=1))
    p.append(circle(cx, cy, 10*scale, center, stroke=P['line'], sw=1))
    p.append(circle(cx-3*scale, cy-2*scale, 1.8*scale, P['ink']))
    p.append(circle(cx+3*scale, cy-2*scale, 1.8*scale, P['ink']))
    p.append(line(cx-4*scale, cy+4*scale, cx+4*scale, cy+4*scale, P['ink'], sw=1.6))
    return ''.join(p)


def leaf(cx, cy, fill='#DCEAD9'):
    return ''.join([
        f'<path d="M {cx} {cy} C {cx+18} {cy-16}, {cx+26} {cy-2}, {cx+8} {cy+10} C {cx-10} {cy+20}, {cx-8} {cy-2}, {cx} {cy} Z" fill="{fill}" stroke="{P["line"]}" stroke-width="1.5"/>',
        line(cx, cy, cx+4, cy+10, P['mint_dark'], 2)
    ])


def pill(x, y, w, label, fill, fg):
    return rect(x, y, w, 38, fill, rx=18, stroke=P['line'], sw=1.2) + text(x + w/2, y + 25, label, 'small', fill=fg, anchor='middle')


def phone_shell(inner):
    x, y, w, h = 140, 120, 920, 1960
    return ''.join([
        rect(0, 0, 1200, 2400, P['bg'], rx=0),
        circle(1060, 220, 220, P['pink'], opacity=0.32),
        circle(180, 2100, 260, P['mint'], opacity=0.5),
        circle(1080, 1870, 150, P['butter'], opacity=0.26),
        rect(x, y, w, h, P['night'], rx=72),
        rect(x+18, y+18, w-36, h-36, P['paper'], rx=58),
        rect(x+330, y+44, 260, 30, '#1C1A1F', rx=15),
        inner,
    ])


def base_screen(title_cn, en, subtitle, nav_idx, tone='pink'):
    left, top, width = 196, 214, 808
    bg = P['pink'] if tone == 'pink' else P['mint'] if tone == 'mint' else P['sky']
    fg = P['pink_dark'] if tone == 'pink' else P['mint_dark'] if tone == 'mint' else P['sky_dark']
    parts = [
        rect(left, top, width, 250, bg, rx=36, stroke=P['line']),
        flower(left + 708, top + 70, petal=P['pink'], center=P['butter'], scale=1.0),
        leaf(left + 640, top + 120, fill=P['mint']),
        text(left + 34, top + 52, en, 'label', fill=fg),
        text(left + 34, top + 120, title_cn, 'h1'),
        text(left + 34, top + 166, subtitle, 'body'),
        pill(left + 34, top + 194, 124, '柔和模式', P['paper'], fg),
        pill(left + 168, top + 194, 136, '快速记录', fg, '#FFFFFF'),
    ]
    nav_titles = ['Today', 'Journal', 'Trends', 'Vault']
    nav_x = [250, 430, 620, 814]
    nav_y = 1894
    parts.append(rect(226, nav_y - 18, 748, 118, P['card'], rx=34, stroke=P['line']))
    for i, (x, label) in enumerate(zip(nav_x, nav_titles)):
        selected = i == nav_idx
        tone_fill = [P['pink'], P['mint'], P['sky'], P['butter']][i]
        tone_fg = [P['pink_dark'], P['mint_dark'], P['sky_dark'], P['butter_dark']][i]
        if selected:
            parts.append(rect(x - 44, nav_y - 4, 138, 88, tone_fill, rx=30, stroke=P['line']))
        parts.append(circle(x + 16, nav_y + 22, 16, tone_fg if selected else '#CCBEB4'))
        parts.append(text(x + 16, nav_y + 66, label, 'nav', fill=tone_fg if selected else P['muted'], anchor='middle'))
    return parts


def make_home():
    p = base_screen('今日总览', 'HEALING TODAY', '像写手帐一样，轻轻记录今天。', 0, 'pink')
    p += [
        rect(196, 504, 808, 332, P['card'], rx=38, stroke=P['line']),
        text(232, 552, '今日节律', 'label', fill=P['pink_dark']),
        text(232, 620, '卵泡期，状态比较轻盈', 'h2'),
        text(232, 664, '今天适合补睡眠、情绪和白带记录。', 'body'),
        circle(836, 654, 108, 'none', stroke=P['pink_dark'], sw=16),
        circle(836, 654, 82, 'none', stroke=P['mint_dark'], sw=12, opacity=0.45),
        text(836, 646, '17', 'h2', fill=P['pink_dark'], anchor='middle'),
        text(836, 680, '天', 'small', anchor='middle'),
        pill(232, 710, 192, '易孕窗 3.22 - 3.27', P['mint'], P['mint_dark']),
        pill(436, 710, 180, '记录完整度 82%', P['butter'], P['butter_dark']),
        text(196, 894, '今天想记什么', 'h2'),
        rect(196, 934, 808, 182, P['card'], rx=34, stroke=P['line']),
    ]
    cards = [('流量','小水滴'), ('疼痛','小太阳'), ('情绪','小笑脸'), ('爱爱','小心心')]
    colors = [P['pink_dark'], P['mint_dark'], P['butter_dark'], '#D37F66']
    for i, (a,b) in enumerate(cards):
        x = 226 + i*188
        p += [rect(x, 968, 160, 112, P['paper'], rx=28, stroke=P['line']), circle(x+36, 1018, 20, colors[i]), text(x+92, 1008, a, 'h3', anchor='middle'), text(x+92, 1040, b, 'small', anchor='middle')]
    p += [
        text(196, 1170, '本周小节律', 'h2'),
        rect(196, 1212, 808, 248, P['card'], rx=34, stroke=P['line']),
        flower(918, 1266, scale=0.8),
    ]
    for i, d in enumerate(['一','二','三','四','五','六','日']):
        x = 246 + i*104
        fill = [P['pink'], P['pink'], P['paper'], P['mint'], P['mint'], P['butter'], P['paper']][i]
        p += [rect(x, 1280, 82, 136, fill, rx=24, stroke=P['line']), text(x+41, 1320, d, 'small', anchor='middle'), text(x+41, 1372, str(18+i), 'h3', anchor='middle')]
    return svg_doc(1200, 2400, phone_shell(''.join(p)))


def make_journal():
    p = base_screen('记录中心', 'JOURNAL NOTE', '把月历、状态和记录入口放进同一本手帐。', 1, 'mint')
    p += [
        rect(196, 504, 808, 420, P['card'], rx=38, stroke=P['line']),
        text(232, 552, '2026 年 3 月', 'label', fill=P['mint_dark']),
        text(232, 610, '本月记录月历', 'h2'),
        text(232, 648, '左右滑动换月，今天的状态做成一张贴纸卡。', 'body'),
    ]
    sx, sy = 232, 692
    n = 1
    for row in range(5):
        for col in range(7):
            x = sx + col*96
            y = sy + row*56
            fill = P['paper']
            if row == 2 and col in (1,2): fill = P['pink']
            if row == 3 and col in (4,5): fill = P['mint']
            p += [rect(x, y, 74, 42, fill, rx=16, stroke=P['line']), text(x+37, y+28, str(n), 'small', anchor='middle')]
            n += 1
    p += [
        rect(196, 974, 808, 164, P['pink'], rx=30, stroke=P['line']),
        flower(930, 1030, scale=0.75),
        text(232, 1022, '今日状态', 'label', fill=P['pink_dark']),
        text(232, 1076, '月经来了 / 月经走了', 'h3'),
        text(232, 1112, '先记状态，再补当天指标。', 'small'),
        pill(696, 1028, 118, '来了', P['pink_dark'], '#FFFFFF'),
        pill(826, 1028, 118, '走了', P['paper'], P['pink_dark']),
        text(196, 1192, '快速记录卡片', 'h2'),
        rect(196, 1234, 808, 332, P['card'], rx=34, stroke=P['line']),
    ]
    names = ['流量', '疼痛', '情绪', '睡眠', '白带', '爱爱']
    tones = [P['pink'], P['butter'], P['pink'], P['sky'], P['mint'], '#F6D8C7']
    for i, name in enumerate(names):
        row, col = divmod(i, 2)
        x = 226 + col*392
        y = 1278 + row*96
        p += [rect(x, y, 362, 78, tones[i], rx=24, stroke=P['line']), text(x+26, y+48, name, 'h3'), text(x+300, y+48, '+', 'h2', fill=P['ink'], anchor='middle')]
    p += [
        text(196, 1622, '同房记录', 'h2'),
        rect(196, 1664, 808, 176, P['card'], rx=34, stroke=P['line']),
        text(232, 1722, '20:40 · 避孕套', 'h3'),
        text(232, 1762, '措施支持多选，其他说明单独输入。', 'body'),
        flower(922, 1728, scale=0.65),
    ]
    return svg_doc(1200, 2400, phone_shell(''.join(p)))


def make_trends():
    p = base_screen('趋势页', 'TRENDS DIARY', '像翻看手帐一样看最近的身体小波动。', 2, 'sky')
    p += [
        rect(196, 504, 808, 220, P['card'], rx=38, stroke=P['line']),
        flower(926, 570, petal=P['sky'], center=P['butter'], scale=0.78),
        text(232, 554, '趋势贴纸', 'label', fill=P['sky_dark']),
        text(232, 610, '最近 30 天有 82% 的记录覆盖', 'h2'),
        text(232, 650, '情绪和睡眠有同步波动，可以继续观察。', 'body'),
        rect(196, 786, 808, 260, P['card'], rx=34, stroke=P['line']),
        text(232, 836, '小趋势', 'label', fill=P['butter_dark']),
        text(232, 890, '睡眠 / 情绪 / 白带', 'h3'),
        line(260, 982, 400, 920, P['pink_dark'], 8)
        + line(400, 920, 560, 958, P['mint_dark'], 8)
        + line(560, 958, 728, 904, P['butter_dark'], 8)
        + line(728, 904, 892, 944, P['sky_dark'], 8),
        line(252, 996, 948, 996, P['line'], 3),
        rect(196, 1108, 808, 234, P['mint'], rx=34, stroke=P['line']),
        text(232, 1160, '照护建议', 'label', fill=P['mint_dark']),
        text(232, 1218, '补充睡眠记录，趋势会更温柔地说话', 'h3'),
        text(232, 1260, '卵泡期样本稳定，适合一起记体温和白带。', 'body'),
        pill(232, 1298, 156, '补 3 天睡眠', P['paper'], P['mint_dark']),
        pill(404, 1298, 144, '记录体温', P['butter'], P['butter_dark']),
        rect(196, 1404, 808, 216, P['card'], rx=34, stroke=P['line']),
        text(232, 1458, '红旗信号', 'label', fill=P['pink_dark']),
        text(232, 1514, '经期若推迟超过 7 天，建议尽快验孕或就医', 'h3'),
        text(232, 1554, '把重要提醒做成固定贴纸，不埋在长列表里。', 'body'),
    ]
    return svg_doc(1200, 2400, phone_shell(''.join(p)))


def make_vault():
    p = base_screen('安全中心', 'VAULT', '像整理小抽屉一样管理提醒、备份和隐私。', 3, 'pink')
    p += [
        rect(196, 504, 808, 218, P['card'], rx=38, stroke=P['line']),
        flower(928, 566, scale=0.8),
        text(232, 554, '状态概览', 'label', fill=P['pink_dark']),
        text(232, 612, '本地优先 · 已开启 PIN', 'h2'),
        text(232, 652, '提醒开启，通知隐藏敏感词，最近一次导出 3 天前。', 'body'),
    ]
    blocks = [
        ('周期设置', '28 天周期 · 经期 5 天 · 最近开始 3 月 11 日', P['mint']),
        ('隐私与解锁', 'PIN 已设置 · 后台 5 分钟自动锁定', P['pink']),
        ('每日提醒', '21:00 · 提前 2 天提醒 · 敏感词隐藏', P['butter']),
        ('备份与恢复', '导出记录、指标与周期设置，不包含密码明文。', P['sky']),
    ]
    y = 780
    for title, sub, fill in blocks:
        p += [rect(196, y, 808, 164, fill, rx=32, stroke=P['line']), text(232, y+56, title, 'h3'), text(232, y+98, sub, 'body'), text(920, y+80, '›', 'h1', fill=P['ink'], anchor='middle')]
        y += 194
    p += [
        rect(196, 1560, 808, 196, P['card'], rx=34, stroke=P['line']),
        text(232, 1616, '数据健康度', 'label', fill=P['mint_dark']),
        text(232, 1674, '73%', 'h2'),
        rect(232, 1696, 736, 22, '#F2E8DD', rx=11),
        rect(232, 1696, 536, 22, P['mint_dark'], rx=11),
        text(232, 1746, '建议先做一次导出，给自己留一份安心副本。', 'small'),
    ]
    return svg_doc(1200, 2400, phone_shell(''.join(p)))


def make_board():
    c = [
        rect(0, 0, 2400, 1600, P['bg'], rx=0),
        circle(2140, 160, 220, P['pink'], opacity=0.32),
        circle(240, 1380, 280, P['mint'], opacity=0.45),
        text(110, 118, 'JAPANESE HEALING STYLE', 'label', fill=P['pink_dark']),
        text(110, 198, '日系治愈手绘风', 'h1'),
        text(110, 248, '奶油纸底、手帐贴纸、樱花粉与浅抹茶，重点是温柔记录而不是功能面板。', 'body'),
        rect(110, 330, 840, 520, P['card'], rx=40, stroke=P['line']),
        flower(852, 396, scale=0.72),
        text(150, 398, '风格关键词', 'h2'),
    ]
    items = ['手帐页感', '手绘贴纸', '小花与叶子', '软边卡片', '轻盈配色', '日常照护感']
    y = 468
    for i, item in enumerate(items):
        c += [circle(168, y-8, 6, [P['pink_dark'], P['mint_dark'], P['butter_dark']][i % 3]), text(190, y, item, 'body')]
        y += 56
    c += [
        rect(110, 888, 840, 518, P['card'], rx=40, stroke=P['line']),
        text(150, 952, '页面结构', 'h2')
    ]
    blocks = [('Today', P['pink']), ('Journal', P['mint']), ('Trends', P['sky']), ('Vault', P['butter'])]
    positions = [(150, 1018), (520, 1018), (150, 1178), (520, 1178)]
    for (label, fill), (x, y) in zip(blocks, positions):
        c += [rect(x, y, 310, 112, fill, rx=28, stroke=P['line']), text(x+24, y+44, label, 'h3'), text(x+24, y+80, '贴纸式内容卡 + 柔和插画', 'small')]

    def mini(px, py, title, tint):
        return ''.join([
            rect(px, py, 282, 574, P['night'], rx=36),
            rect(px+10, py+10, 262, 554, P['paper'], rx=28),
            rect(px+26, py+36, 230, 102, tint, rx=24, stroke=P['line']),
            text(px+46, py+82, title, 'h3'),
            flower(px+218, py+84, scale=0.42),
            rect(px+28, py+164, 226, 118, P['card'], rx=20, stroke=P['line']),
            rect(px+28, py+302, 226, 92, P['card'], rx=20, stroke=P['line']),
            rect(px+28, py+414, 226, 112, P['card'], rx=20, stroke=P['line'])
        ])

    c += [text(1070, 390, '新版 mockup', 'h2')]
    c += [mini(1070, 430, 'Today', P['pink'])]
    c += [mini(1388, 430, 'Journal', P['mint'])]
    c += [mini(1706, 430, 'Trends', P['sky'])]
    c += [mini(2024, 430, 'Vault', P['butter'])]
    c += [
        rect(1070, 1060, 1236, 346, P['card'], rx=40, stroke=P['line']),
        text(1110, 1122, '色板', 'h2'),
        rect(1110, 1178, 150, 80, P['pink'], rx=24),
        rect(1280, 1178, 150, 80, P['mint'], rx=24),
        rect(1450, 1178, 150, 80, P['butter'], rx=24),
        rect(1620, 1178, 150, 80, P['sky'], rx=24),
        rect(1790, 1178, 150, 80, P['paper'], rx=24, stroke=P['line']),
        text(1110, 1338, '资源目录已接入品牌花朵、空状态和启动页插画。', 'body')
    ]
    return svg_doc(2400, 1600, ''.join(c))


files = {
    'zhiqi_redesign_home.svg': make_home(),
    'zhiqi_redesign_journal.svg': make_journal(),
    'zhiqi_redesign_trends.svg': make_trends(),
    'zhiqi_redesign_vault.svg': make_vault(),
    'zhiqi_redesign_board.svg': make_board(),
}
for name, data in files.items():
    (OUT / name).write_text(data, encoding='utf-8')
    print(name)
