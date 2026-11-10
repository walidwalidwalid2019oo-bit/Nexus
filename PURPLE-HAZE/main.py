import pygame
import sys
import math

pygame.init()
pygame.display.set_caption("PURPLE HAZE")
screen = pygame.display.set_mode((960, 540), pygame.RESIZABLE)
clock = pygame.time.Clock()

PURPLE = (119, 75, 190)
DARK = (17, 12, 35)
LILAC = (205, 170, 255)
WHITE = (245, 238, 255)
GOLD = (255, 220, 130)

world_width = 2300
player = pygame.Rect(100, 330, 34, 48)
vx = 0
vy = 0
on_ground = False
camera_x = 0
crystals = [pygame.Rect(470, 330, 22, 22), pygame.Rect(1020, 260, 22, 22), pygame.Rect(1670, 310, 22, 22)]
platforms = [
    pygame.Rect(0, 410, 520, 70),
    pygame.Rect(590, 360, 300, 55),
    pygame.Rect(950, 320, 300, 55),
    pygame.Rect(1320, 390, 330, 55),
    pygame.Rect(1710, 370, 520, 90),
]
echo_rect = pygame.Rect(1080, 270, 30, 45)
portal = pygame.Rect(2110, 300, 65, 100)
talk = False
won = False
font = pygame.font.Font(None, 30)
small = pygame.font.Font(None, 22)

def draw_text(surface, msg, x, y, color=WHITE, f=font):
    surface.blit(f.render(msg, True, color), (x, y))

def button_rect(name, w, h):
    # Controls are placed relative to the actual display size.
    sw, sh = screen.get_size()
    if name == "left": return pygame.Rect(18, sh-h-20, w, h)
    if name == "right": return pygame.Rect(18+w+12, sh-h-20, w, h)
    if name == "jump": return pygame.Rect(sw-w-22, sh-h-20, w, h)
    return pygame.Rect(sw-w-22, sh-h*2-34, w, h)

def draw_button(label, rect):
    pygame.draw.rect(screen, (72, 48, 112), rect, border_radius=14)
    pygame.draw.rect(screen, (177, 139, 235), rect, 2, border_radius=14)
    txt = small.render(label, True, WHITE)
    screen.blit(txt, txt.get_rect(center=rect.center))

def say_echo():
    global talk
    talk = not talk

while True:
    dt = min(clock.tick(60) / 1000, 0.03)
    sw, sh = screen.get_size()
    scale = min(sw / 960, sh / 540)
    view_w, view_h = int(960*scale), int(540*scale)
    canvas = pygame.Surface((960, 540))
    keys = pygame.key.get_pressed()
    move_left = keys[pygame.K_LEFT] or keys[pygame.K_a]
    move_right = keys[pygame.K_RIGHT] or keys[pygame.K_d]
    jump_pressed = keys[pygame.K_SPACE] or keys[pygame.K_UP] or keys[pygame.K_w]
    echo_pressed = False

    left_btn = pygame.Rect(18, 450, 72, 64)
    right_btn = pygame.Rect(102, 450, 72, 64)
    jump_btn = pygame.Rect(850, 450, 88, 64)
    echo_btn = pygame.Rect(850, 374, 88, 62)

    for event in pygame.event.get():
        if event.type == pygame.QUIT:
            pygame.quit()
            sys.exit()
        if event.type == pygame.VIDEORESIZE:
            screen = pygame.display.set_mode((max(480,event.w), max(270,event.h)), pygame.RESIZABLE)
        if event.type == pygame.KEYDOWN and event.key == pygame.K_e:
            echo_pressed = True
        if event.type in (pygame.MOUSEBUTTONDOWN, pygame.FINGERDOWN):
            if event.type == pygame.FINGERDOWN:
                px, py = int(event.x * 960), int(event.y * 540)
            else:
                # Convert actual screen coordinates into the 960x540 game canvas.
                px = int(event.pos[0] * 960 / max(1, sw))
                py = int(event.pos[1] * 540 / max(1, sh))
            if left_btn.collidepoint(px,py): move_left = True
            if right_btn.collidepoint(px,py): move_right = True
            if jump_btn.collidepoint(px,py): jump_pressed = True
            if echo_btn.collidepoint(px,py): echo_pressed = True

    vx = (-230 if move_left else 0) + (230 if move_right else 0)
    player.x += int(vx * dt)
    player.x = max(0, min(world_width-player.width, player.x))

    if jump_pressed and on_ground:
        vy = -510
        on_ground = False
    vy += 1250 * dt
    player.y += int(vy * dt)
    on_ground = False
    for plat in platforms:
        if player.colliderect(plat) and vy >= 0 and player.bottom - vy*dt <= plat.top + 8:
            player.bottom = plat.top
            vy = 0
            on_ground = True
    if player.bottom > 520:
        player.bottom = 520
        vy = 0
        on_ground = True

    camera_x = max(0, min(world_width-960, player.centerx-480))
    collected = 3 - len(crystals)
    near_echo = abs(player.centerx-echo_rect.centerx) < 100
    if echo_pressed and near_echo:
        say_echo()
    if len(crystals) == 0 and player.colliderect(portal):
        won = True

    # Background
    for y in range(540):
        t = y/540
        col = (int(18+18*t), int(12+8*t), int(43+35*t))
        pygame.draw.line(canvas, col, (0,y), (960,y))
    for i in range(45):
        x = (i*197 - int(camera_x*0.22)) % 960
        y = (i*83) % 300
        pygame.draw.circle(canvas, (150,125,190), (x,y), 1)

    # Distant moon and island
    pygame.draw.circle(canvas, (83,55,132), (760,110), 62)
    pygame.draw.circle(canvas, (145,104,190), (740,90), 10)
    for p in platforms:
        r = pygame.Rect(p.x-camera_x, p.y, p.width, p.height)
        pygame.draw.rect(canvas, (49,31,78), r, border_radius=12)
        pygame.draw.rect(canvas, (137,93,185), (r.x,r.y,r.width,8), border_radius=5)

    for c in crystals:
        pygame.draw.polygon(canvas, GOLD, [(c.centerx-camera_x,c.top),(c.right-camera_x,c.centery),(c.centerx-camera_x,c.bottom),(c.left-camera_x,c.centery)])
        pygame.draw.polygon(canvas, WHITE, [(c.centerx-camera_x,c.top+3),(c.centerx-camera_x+5,c.centery),(c.centerx-camera_x,c.bottom-4)])
    crystals[:] = [c for c in crystals if not player.colliderect(c)]

    er = pygame.Rect(echo_rect.x-camera_x, echo_rect.y, echo_rect.width, echo_rect.height)
    pygame.draw.circle(canvas, LILAC, (er.centerx, er.y+5), 15)
    pygame.draw.rect(canvas, (105,77,155), (er.x,er.y+18,er.width,27), border_radius=8)
    pr = pygame.Rect(portal.x-camera_x, portal.y, portal.width, portal.height)
    pygame.draw.ellipse(canvas, (102,63,174), pr, 7)
    pygame.draw.ellipse(canvas, LILAC, pr.inflate(-18,-15), 3)

    pygame.draw.rect(canvas, (225,205,255), (player.x-camera_x,player.y,player.width,player.height), border_radius=8)
    draw_text(canvas, f"CRYSTALS: {collected}/3", 22, 18)
    draw_text(canvas, "MISSION: Find 3 crystals and reach the portal", 22, 48, LILAC, small)

    # Mini-map
    pygame.draw.rect(canvas, (38,25,62), (790,18,148,28), border_radius=8)
    marker = 790 + int(player.centerx/world_width*140)
    pygame.draw.circle(canvas, GOLD, (marker,32), 5)

    draw_button("◀", left_btn)
    draw_button("▶", right_btn)
    draw_button("ECHO", echo_btn)
    draw_button("JUMP", jump_btn)

    if near_echo:
        draw_text(canvas, "Press E / ECHO to talk", 360, 395, LILAC, small)
    if talk:
        pygame.draw.rect(canvas, (28,18,48), (235,340,500,72), border_radius=12)
        pygame.draw.rect(canvas, LILAC, (235,340,500,72), 2, border_radius=12)
        draw_text(canvas, "ECHO: The crystals remember the way. Follow their light.", 252, 363, WHITE, small)
    if won:
        pygame.draw.rect(canvas, (25,15,48), (250,205,460,100), border_radius=16)
        draw_text(canvas, "PORTAL RESTORED", 370, 225, GOLD)
        draw_text(canvas, "The first chapter is complete.", 350, 263, WHITE, small)

    # Scale game canvas to fit the real phone/window dimensions.
    scaled = pygame.transform.smoothscale(canvas, (view_w, view_h))
    screen.fill((8,6,18))
    screen.blit(scaled, ((sw-view_w)//2, (sh-view_h)//2))
    pygame.display.flip()
