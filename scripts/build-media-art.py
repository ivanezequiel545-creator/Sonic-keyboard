#!/usr/bin/env python3
"""Regenerate Velocity's original offline media (Pillow required only for regeneration).
No external images, characters, fonts distributed, APIs or copyrighted franchises.
"""
from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math,json
ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'public/media';OUT.mkdir(exist_ok=True)
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
items=[]
labels=[('hola','¡Hola!',(60,116,255)),('gracias','Gracias',(0,155,147)),('si','¡Sí!',(139,82,232)),('no','Hoy no',(232,95,129)),('vamos','¡Vamos!',(238,133,36)),('genial','Genial',(41,144,200))]
def art(label,color,phase=0,gif=False):
    im=Image.new('RGBA',(240,240),(17,28,53,255) if gif else (0,0,0,0));d=ImageDraw.Draw(im)
    y=int(7*math.sin(phase))
    d.rounded_rectangle((14,20+y,226,212+y),radius=48,fill=color+(255,),outline=(255,255,255,255),width=5)
    d.ellipse((78,51+y,162,135+y),fill=(255,230,110,255))
    d.ellipse((99,78+y,106,88+y),fill=(25,36,62,255));d.ellipse((135,78+y,142,88+y),fill=(25,36,62,255))
    d.arc((102,86+y,140,115+y),0,180,fill=(25,36,62,255),width=4)
    for x,sy in [(43,59),(195,112),(53,141)]:
        r=6+int(2*math.sin(phase+x));d.polygon([(x,sy-r),(x+3,sy-3),(x+r,sy),(x+3,sy+3),(x,sy+r),(x-3,sy+3),(x-r,sy),(x-3,sy-3)],fill=(255,255,255,255))
    f=ImageFont.truetype(FONT,29);box=d.textbbox((0,0),label,font=f);w=box[2]-box[0]
    d.text(((240-w)/2,156+y),label,font=f,fill='white',stroke_width=0)
    return im
for name,label,color in labels:
    file=name+'.png';art(label,color).save(OUT/file,optimize=True)
    items.append(dict(id=name,label=label,kind='sticker',mime='image/png',file=file))
for name,label,color in labels[:4]:
    file=name+'.gif';frames=[art(label,color,2*math.pi*i/16,True).convert('RGB').quantize(colors=64) for i in range(16)]
    frames[0].save(OUT/file,save_all=True,append_images=frames[1:],duration=90,loop=0,disposal=2,optimize=True)
    items.append(dict(id=name+'-gif',label=label+' animado',kind='gif',mime='image/gif',file=file))
(OUT/'catalog.json').write_text(json.dumps(items,ensure_ascii=False,indent=2)+'\n')
