import math
import random
import sys
import pygame

# PURPLE HAZE 2.0 — phone-first playable vertical-slice prototype
pygame.init()
pygame.display.set_caption("PURPLE HAZE 2.0")
W, H = 960, 540
screen = pygame.display.set_mode((W, H), pygame.RESIZABLE)
clock = pygame.time.Clock()

# Palette
INK = (8, 6, 18)
NIGHT = (16, 10, 31)
VIOLET = (119, 65, 190)
PURPLE = (154, 91, 230)
LILAC = (210, 177, 255)
PALE = (242, 229, 255)
GOLD = (255, 220, 145)
MIST = (72, 45, 105)

font = pygame.font.Font(None, 30)
small = pygame.font.Font(None, 21)
large = pygame.font.Font(None, 72)
tiny = pygame.font.Font(None, 17)

def clamp(v, lo, hi):
    return max(lo, min(hi, v))

def text(surf, msg, pos, color=PALE, face=font):
    surf.blit(face.render(msg, True, color), pos)

def glow(surf, pos, radius, color, alpha=55):
    layer = pygame.Surface((radius * 4, radius * 4), pygame.SRCALPHA)
    for i in range(4, 0, -1):
        r = int(radius * i / 2)
        a = int(alpha / (i * 0.8))
        pygame.draw.circle(layer, (*color, a), (radius * 2, radius * 2), r)
    surf.blit(layer, (int(pos[0] - radius * 2), int(pos[1] - radius * 2)))

def rounded(surf, rect, color, radius=12, border=None, width=2):
    pygame.draw.rect(surf, color, rect, border_radius=radius)
    if border:
        pygame.draw.rect(surf, border, rect, width, border_radius=radius)

# World layout
WORLD_W = 3000
FLOOR_Y = 470
platforms = [
    pygame.Rect(0, 440, 560, 100),
    pygame.Rect(650, 390, 300, 42),
    pygame.Rect(1030, 340, 280, 42),
    pygame.Rect(1390, 405, 310, 42),
    pygame.Rect(1780, 355, 300, 42),
    pygame.Rect(2180, 410, 360, 42),
    pygame.Rect(2630, 440, 370, 100),
]
crystal_spawns = [(420, 395), (1120, 295), (1900, 310), (2370, 365)]
echo_box = pygame.Rect(1500, 340, 40, 65)
portal = pygame.Rect(2820, 335, 85, 105)

player = pygame.Rect(110, 350, 36, 58)
spawn = (110, 350)
vx = vy = 0.0
grounded = False
camera_x = 0.0
facing = 1
dash_cooldown = 0.0
dash_time = 0.0
invuln = 0.0
health = 3
coins = 0
owned_weapons = ["Shadow Blade"]
selected_weapon = "Shadow Blade"
shop_open = False
attack_time = 0.0
attack_cooldown = 0.0
game_state = "menu"
dialogue_open = False
dialogue_index = 0
won = False
elapsed = 0.0

crystals = [pygame.Rect(x, y, 22, 34) for x, y in crystal_spawns]
particles = []
stars = [(random.randrange(WORLD_W), random.randrange(20, 330), random.choice([1, 1, 2])) for _ in range(130)]
echo_lines = [
    "You have returned to a place that remembers you.",
    "The lights are fragments of the Veil. Gather them.",
    "The little machine beside you has lost more than it knows.",
    "When the gate wakes, do not mistake it for an answer."
]

# HILPO companion
hilpo_x, hilpo_y = 155.0, 330.0
hilpo_angle = 0.0
hilpo_mode = "FOLLOW"
hilpo_message = ""
hilpo_message_time = 0.0
scan_time = 0.0

# Input: edge-triggered buttons prevent accidental repeated actions
finger_points = {}
mouse_down = False
mouse_pos = (0, 0)
jump_was_down = False
interact_was_down = False
dash_was_down = False

def controls():
    return {
        "left": pygame.Rect(22, 438, 76, 76),
        "right": pygame.Rect(108, 438, 76, 76),
        "jump": pygame.Rect(850, 438, 88, 76),
        "action": pygame.Rect(850, 350, 88, 72),
        "scan": pygame.Rect(750, 438, 82, 76),
        "attack": pygame.Rect(650, 350, 88, 72),
        "shop": pygame.Rect(790, 70, 130, 38),
    }

def draw_button(surf, label, rect, active=False):
    fill = (91, 54, 137) if active else (31, 22, 51)
    rounded(surf, rect, fill, 15, (155, 112, 213), 2)
    label_img = small.render(label, True, PALE)
    surf.blit(label_img, label_img.get_rect(center=rect.center))

def add_particles(x, y, color=PURPLE, count=12, speed=100):
    for _ in range(count):
        a = random.random() * math.tau
        sp = random.uniform(speed * .25, speed)
        particles.append([float(x), float(y), math.cos(a)*sp, math.sin(a)*sp,
                          random.uniform(.35, .8), color, random.randint(2, 5)])

def update_particles(dt):
    for p in particles[:]:
        p[0] += p[2] * dt
        p[1] += p[3] * dt
        p[4] -= dt
        p[3] += 35 * dt
        if p[4] <= 0:
            particles.remove(p)

def resolve_x(rect, dx):
    rect.x += int(dx)
    for block in platforms:
        if rect.colliderect(block):
            if dx > 0:
                rect.right = block.left
            elif dx < 0:
                rect.left = block.right

def resolve_y(rect, dy):
    global vy, grounded
    rect.y += int(dy)
    grounded = False
    for block in platforms:
        if rect.colliderect(block):
            if dy > 0:
                rect.bottom = block.top
                grounded = True
            elif dy < 0:
                rect.top = block.bottom
            vy = 0

def world_to_screen(x, y):
    return int(x - camera_x), int(y)

def draw_background(surf):
    for y in range(H):
        t = y / H
        pygame.draw.line(surf, (int(9+13*t), int(7+8*t), int(25+30*t)), (0,y), (W,y))
    # distant planet
    px = int(770 - camera_x * .035)
    glow(surf, (px, 110), 65, (112, 70, 170), 35)
    pygame.draw.circle(surf, (52, 35, 83), (px, 110), 48)
    pygame.draw.arc(surf, (121, 83, 166), (px-54, 83, 108, 55), .1, 2.8, 2)
    for sx, sy, size in stars:
        x = int((sx - camera_x * .18) % WORLD_W)
        x = x % W
        if (sx + int(camera_x*.18)) % 5 == 0:
            pygame.draw.circle(surf, (110, 91, 151), (x, sy), size)
    # atmospheric haze bands
    haze = pygame.Surface((W,H), pygame.SRCALPHA)
    for i in range(5):
        yy = 245 + i*35 + int(math.sin(elapsed*.35+i)*10)
        pygame.draw.ellipse(haze, (95, 49, 135, 12), (-100, yy, 1180, 100))
    surf.blit(haze, (0,0))

def draw_platforms(surf):
    for p in platforms:
        r = pygame.Rect(p.x-camera_x, p.y, p.width, p.height)
        if r.right < -20 or r.left > W+20:
            continue
        # island shadow and layered rock
        pygame.draw.ellipse(surf, (5,4,14), (r.x-8, r.bottom-10, r.width+16, 32))
        pygame.draw.rect(surf, (35, 24, 55), r, border_radius=9)
        pygame.draw.rect(surf, (70, 43, 99), (r.x, r.y, r.width, 10), border_radius=5)
        pygame.draw.line(surf, (145, 91, 184), (r.x+12,r.y+2), (r.right-12,r.y+2), 2)
        for j in range(r.x+20, r.right-10, 58):
            pygame.draw.polygon(surf, (48,32,69), [(j,r.bottom-5),(j+10,r.bottom+13),(j+22,r.bottom-5)])

def draw_crystals(surf):
    for c in crystals:
        x, y = world_to_screen(c.centerx, c.centery)
        glow(surf, (x,y), 17, GOLD, 55)
        pts = [(x,y-20),(x+12,y),(x,y+20),(x-12,y)]
        pygame.draw.polygon(surf, (255,205,125), pts)
        pygame.draw.polygon(surf, (255,245,213), [(x,y-14),(x+4,y),(x,y+11)])
        pygame.draw.line(surf, (255,255,245), (x,y-13),(x,y+10), 2)

def draw_hollow(surf):
    x, y = world_to_screen(player.centerx, player.centery)
    moving = abs(vx) > 20
    bob = math.sin(elapsed*(10 if moving else 4))* (5 if moving else 3)
    lean = int(clamp(vx/55, -4, 4))
    glow(surf, (x,y), 34, (139,68,220), 60)
    # smoke trail
    for i in range(5):
        sx = x - facing*(10+i*9)
        sy = int(y+12+i*4+math.sin(elapsed*5+i)*5)
        pygame.draw.circle(surf, (65,35,97), (sx,sy), max(2,9-i))
    body = [(x+lean, int(y-30+bob)), (x+15+lean,int(y-17+bob)), (x+13+lean,int(y+18+bob)),
            (x+lean,int(y+29+bob)), (x-13+lean,int(y+18+bob)), (x-15+lean,int(y-17+bob))]
    pygame.draw.polygon(surf, (13,9,23), body)
    pygame.draw.polygon(surf, (116,67,166), body, 2)
    pygame.draw.line(surf, (192,139,255), (x+lean+facing*5,y-10),(x+lean+facing*5,y+11), 2)
    if attack_time > 0:
        tip = (x + facing*58, y-12)
        base = (x + facing*13, y+8)
        glow(surf, tip, 23, (166,91,255), 90)
        if selected_weapon == "Veil Scythe":
            pygame.draw.arc(surf, (210,170,255), (min(base[0],tip[0])-12,y-58,72,88), 3.8, 5.9, 7)
        elif selected_weapon == "Void Gauntlet":
            pygame.draw.circle(surf, (197,132,255), (x+facing*28,y+7), 17, 5)
            pygame.draw.circle(surf, (92,43,155), (x+facing*28,y+7), 9)
        else:
            pygame.draw.line(surf, (224,194,255), base, tip, 8)
            pygame.draw.line(surf, (130,66,220), base, tip, 3)
        pygame.draw.circle(surf, (236,216,255), tip, 5)

def draw_hilpo(surf):
    x, y = int(hilpo_x-camera_x), int(hilpo_y)
    pulse = 2*math.sin(elapsed*5)
    glow(surf, (x,y), 23, (142,76,230), 65)
    pygame.draw.circle(surf, (29,23,47), (x,y), 14)
    pygame.draw.circle(surf, (133,91,183), (x,y), 14, 2)
    pygame.draw.circle(surf, (205,164,255), (x,y), int(5+pulse*.3))
    pygame.draw.arc(surf, (239,220,255), (x-8,y-8,16,16), .3, 2.5, 2)
    # tiny side fins
    pygame.draw.polygon(surf, (91,62,130), [(x-13,y),(x-21,y-5),(x-19,y+6)])
    pygame.draw.polygon(surf, (91,62,130), [(x+13,y),(x+21,y-5),(x+19,y+6)])

def draw_echo(surf):
    x, y = world_to_screen(echo_box.centerx, echo_box.centery)
    pulse = int(4*math.sin(elapsed*2))
    glow(surf, (x,y), 34, (170,120,235), 40)
    pygame.draw.circle(surf, (187,151,224), (x,y-12), 17+pulse, 2)
    pygame.draw.circle(surf, (68,48,96), (x,y-12), 11)
    pygame.draw.line(surf, (179,143,220), (x,y+5),(x,y+30), 3)
    pygame.draw.line(surf, (179,143,220), (x-13,y+15),(x+13,y+15), 2)
    text(surf, "E C H O", (x-27,y-45), LILAC, tiny)

def draw_portal(surf):
    x, y = world_to_screen(portal.centerx, portal.centery)
    glow(surf, (x,y), 55, (130,69,204), 30)
    pygame.draw.ellipse(surf, (76,43,119), (x-34,y-52,68,104), 8)
    pygame.draw.ellipse(surf, (181,132,239), (x-23,y-42,46,84), 3)
    pygame.draw.ellipse(surf, (31,19,53), (x-16,y-34,32,68))
    if not crystals:
        pygame.draw.ellipse(surf, (205,166,255), (x-12,y-29,24,58), 2)

def draw_particles(surf):
    for p in particles:
        x = int(p[0]-camera_x)
        if -10 < x < W+10:
            pygame.draw.circle(surf, p[5], (x,int(p[1])), max(1,int(p[6]*p[4])))

def draw_hud(surf):
    rounded(surf, (18,16,300,62), (20,14,37), 12, (91,62,128), 1)
    text(surf, "VESPER  /  SHATTERED ISLES", (31,25), LILAC, small)
    text(surf, "VEIL SHARDS", (31,49), PALE, tiny)
    for i in range(4):
        col = GOLD if i < 4-len(crystals) else (67,52,83)
        pygame.draw.circle(surf, col, (148+i*25,56), 7)
    # health and Veil Coins
    for i in range(3):
        pygame.draw.circle(surf, (191,111,220) if i < health else (55,42,68), (270+i*18,55), 6)
    rounded(surf, (340,16,132,43), (20,14,37), 10, (126,91,53), 1)
    text(surf, "✦ "+str(coins), (354,27), GOLD, small)
    # HILPO state
    rounded(surf, (780,16,160,43), (20,14,37), 10, (91,62,128), 1)
    pygame.draw.circle(surf, PURPLE, (798,37), 5)
    text(surf, "HILPO  "+hilpo_mode, (811,27), PALE, tiny)

def draw_shop(surf):
    rounded(surf, (190,75,580,390), (15,10,30), 18, (164,112,230), 3)
    text(surf, "VEIL MARKET", (225,96), LILAC, large)
    text(surf, "Coins: "+str(coins)+"   |   B / SHOP to close", (230,153), GOLD, small)
    items = [("Shadow Blade",0,"Balanced starter weapon"),("Veil Scythe",75,"Wide, sweeping shadow arcs"),("Void Gauntlet",120,"Heavy close-range impact")]
    for i,(name,cost,desc) in enumerate(items):
        y = 205+i*75
        selected = selected_weapon == name
        rounded(surf, (220,y,520,60), (47,29,70) if selected else (27,19,45), 10,
                (196,145,255) if selected else (83,61,112), 2)
        text(surf, str(i+1)+". "+name, (238,y+8), PALE, small)
        text(surf, desc, (238,y+33), (173,151,195), tiny)
        status = "EQUIPPED" if selected else ("OWNED" if name in owned_weapons else str(cost)+" COINS")
        text(surf, status, (620,y+20), GOLD if name not in owned_weapons else LILAC, tiny)
    text(surf, "Press 1 / 2 / 3 to buy or equip", (260,433), PALE, tiny)

def buy_or_equip(index):
    global coins, selected_weapon
    items = [("Shadow Blade",0),("Veil Scythe",75),("Void Gauntlet",120)]
    if not 0 <= index < len(items): return
    name,cost = items[index]
    if name not in owned_weapons:
        if coins < cost:
            show_message("HILPO: Not enough Veil Coins.")
            return
        coins -= cost
        owned_weapons.append(name)
        show_message("HILPO: "+name+" formed from shadow.")
    selected_weapon = name

def perform_attack():
    global attack_time, attack_cooldown
    if attack_cooldown > 0 or shop_open or dialogue_open: return
    attack_time = 0.28 if selected_weapon == "Shadow Blade" else 0.4
    attack_cooldown = 0.38 if selected_weapon == "Shadow Blade" else 0.58
    add_particles(player.centerx+facing*35, player.centery, LILAC, 10, 90)

def draw_dialogue(surf, speaker, line):
    rounded(surf, (135,365,690,105), (17,12,32), 14, (159,119,210), 2)
    text(surf, speaker, (158,380), LILAC, small)
    text(surf, line, (158,410), PALE, small)
    text(surf, "TAP ACTION / E TO CONTINUE", (158,447), (157,133,183), tiny)

def draw_menu(surf):
    draw_background(surf)
    # title treatment
    glow(surf, (480,210), 115, (123,61,196), 35)
    title = large.render("PURPLE HAZE", True, PALE)
    surf.blit(title, title.get_rect(center=(480,205)))
    sub = small.render("A WORLD THAT REMEMBERS", True, LILAC)
    surf.blit(sub, sub.get_rect(center=(480,252)))
    rounded(surf, (350,320,260,62), (42,26,66), 15, (167,119,226), 2)
    start = font.render("BEGIN THE AWAKENING", True, PALE)
    surf.blit(start, start.get_rect(center=(480,351)))
    text(surf, "Touch the button or press ENTER", (382,405), (171,151,195), small)
    text(surf, "2.0  •  ANDROID / KEYBOARD", (400,492), (116,95,145), tiny)

def show_message(msg):
    global hilpo_message, hilpo_message_time
    hilpo_message = msg
    hilpo_message_time = 2.5

def interact():
    global dialogue_open, dialogue_index, won
    near_echo = abs(player.centerx-echo_box.centerx)<125 and abs(player.centery-echo_box.centery)<120
    near_portal = abs(player.centerx-portal.centerx)<100 and abs(player.centery-portal.centery)<110
    if dialogue_open:
        dialogue_index += 1
        if dialogue_index >= len(echo_lines):
            dialogue_open = False
        return
    if near_echo:
        dialogue_open = True
        dialogue_index = 0
    elif near_portal:
        if not crystals:
            won = True
            show_message("The gate is awake.")
        else:
            show_message("HILPO: Veil shards missing.")
    else:
        show_message("HILPO: No signal nearby.")

def scan():
    global scan_time
    scan_time = 1.2
    show_message("HILPO: Scanning... Veil signature detected.")
    add_particles(hilpo_x, hilpo_y, LILAC, 18, 75)

def reset_game():
    global player, vx, vy, camera_x, crystals, health, won, dialogue_open, coins, shop_open, attack_time, attack_cooldown
    global hilpo_x, hilpo_y, game_state, particles, dialogue_index
    global grounded, facing, dash_time, dash_cooldown, invuln
    global jump_was_down, interact_was_down, dash_was_down, scan_time
    global hilpo_angle, hilpo_mode, hilpo_message, hilpo_message_time
    player.topleft = spawn
    vx = vy = camera_x = 0
    grounded = False
    facing = 1
    dash_time = dash_cooldown = invuln = scan_time = 0.0
    jump_was_down = interact_was_down = dash_was_down = False
    crystals = [pygame.Rect(x,y,22,34) for x,y in crystal_spawns]
    health = 3
    coins = 0
    shop_open = False
    attack_time = attack_cooldown = 0.0
    won = False
    dialogue_open = False
    dialogue_index = 0
    hilpo_x, hilpo_y = 155.0, 330.0
    hilpo_angle = 0.0
    hilpo_mode = "FOLLOW"
    hilpo_message = ""
    hilpo_message_time = 0.0
    particles = []
    game_state = "play"

def main():
    global screen, vx, vy, grounded, camera_x, facing, dash_time, dash_cooldown
    global invuln, health, elapsed, game_state, dialogue_open, hilpo_x, hilpo_y
    global hilpo_angle, hilpo_mode, hilpo_message_time, scan_time, jump_was_down
    global interact_was_down, dash_was_down, mouse_down, mouse_pos
    global shop_open, attack_time, attack_cooldown, coins
    running = True
    while running:
        dt = min(clock.tick(60)/1000, .03)
        elapsed += dt
        keys = pygame.key.get_pressed()
        sw, sh = screen.get_size()
        btn = controls()
        action_edge = scan_edge = dash_edge = False

        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            elif event.type == pygame.VIDEORESIZE:
                screen = pygame.display.set_mode((max(480,event.w),max(270,event.h)), pygame.RESIZABLE)
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    if dialogue_open: dialogue_open = False
                    elif game_state == "play": game_state = "menu"
                    else: running = False
                elif event.key == pygame.K_RETURN and game_state == "menu":
                    reset_game()
                elif event.key in (pygame.K_e, pygame.K_f):
                    action_edge = True
                elif event.key == pygame.K_q:
                    scan_edge = True
                elif event.key == pygame.K_b:
                    shop_open = not shop_open
                elif event.key == pygame.K_j:
                    perform_attack()
                elif event.key in (pygame.K_1, pygame.K_2, pygame.K_3) and shop_open:
                    buy_or_equip(event.key-pygame.K_1)
                elif event.key in (pygame.K_LSHIFT, pygame.K_RSHIFT):
                    dash_edge = True
            elif event.type == pygame.MOUSEBUTTONDOWN and event.button == 1:
                mouse_down, mouse_pos = True, event.pos
            elif event.type == pygame.MOUSEBUTTONUP and event.button == 1:
                mouse_down = False
            elif event.type == pygame.MOUSEMOTION:
                mouse_pos = event.pos
            elif event.type == pygame.FINGERDOWN:
                # Keep normalized touch coordinates so resizing cannot desync controls.
                finger_points[event.finger_id] = (event.x,event.y)
            elif event.type == pygame.FINGERMOTION:
                finger_points[event.finger_id] = (event.x,event.y)
            elif event.type == pygame.FINGERUP:
                finger_points.pop(event.finger_id,None)

        # Convert window-space pointer positions to the fixed 960x540 game canvas.
        sw, sh = screen.get_size()
        points = [(x*W,y*H) for x,y in finger_points.values()]
        if mouse_down:
            points.append((mouse_pos[0]*W/sw,mouse_pos[1]*H/sh))
        pressed = set()
        for point in points:
            for name, rect in btn.items():
                if rect.collidepoint(point):
                    pressed.add(name)

        if game_state == "menu":
            if points or "action" in pressed:
                reset_game()
            canvas = pygame.Surface((W,H))
            draw_menu(canvas)
        else:
            move_left = keys[pygame.K_LEFT] or keys[pygame.K_a] or "left" in pressed
            move_right = keys[pygame.K_RIGHT] or keys[pygame.K_d] or "right" in pressed
            jump_down = keys[pygame.K_SPACE] or keys[pygame.K_w] or keys[pygame.K_UP] or "jump" in pressed
            action_down = keys[pygame.K_e] or keys[pygame.K_f] or "action" in pressed
            scan_down = keys[pygame.K_q] or "scan" in pressed
            attack_down = keys[pygame.K_j] or "attack" in pressed
            if "shop" in pressed: shop_open = True
            if attack_down and not getattr(main,"attack_was_down",False): perform_attack()
            main.attack_was_down = attack_down
            dash_down = keys[pygame.K_LSHIFT] or keys[pygame.K_RSHIFT]
            action_edge = action_edge or (action_down and not interact_was_down)
            scan_edge = scan_edge or (scan_down and not getattr(main,"scan_was_down",False))
            dash_edge = dash_edge or (dash_down and not dash_was_down)
            interact_was_down = action_down
            main.scan_was_down = scan_down
            dash_was_down = dash_down

            if action_edge and not shop_open: interact()
            if scan_edge and not shop_open: scan()
            if dash_edge and dash_cooldown <= 0 and not dialogue_open and not shop_open:
                dash_time, dash_cooldown = .16, .85
                add_particles(player.centerx,player.centery,PURPLE,12,100)

            if not dialogue_open and not shop_open:
                direction = int(move_right)-int(move_left)
                if direction:
                    facing = direction
                if dash_time > 0:
                    vx = facing*650
                    dash_time = max(0,dash_time-dt)
                else:
                    vx = direction*245
                resolve_x(player,vx*dt)
                if jump_down and not jump_was_down and grounded:
                    vy = -535
                    grounded = False
                    add_particles(player.centerx,player.bottom,(119,75,190),7,55)
                jump_was_down = jump_down
                vy = min(vy+1420*dt,850)
                resolve_y(player,vy*dt)
            else:
                jump_was_down = jump_down

            if player.top > H+120:
                player.topleft = spawn
                vy = 0
                health = max(0,health-1)
            if health <= 0:
                reset_game()

            # HILPO spring-follow with distance clamp and smooth velocity
            target_x = player.centerx - facing*62
            target_y = player.centery - 30 + math.sin(elapsed*3)*10
            dx,dy = target_x-hilpo_x,target_y-hilpo_y
            distance = math.hypot(dx,dy)
            if distance > 260:
                hilpo_x,hilpo_y = target_x,target_y
            else:
                hilpo_x += dx*min(1,dt*4.2)
                hilpo_y += dy*min(1,dt*4.2)
            hilpo_angle = math.atan2(dy,dx)
            if dash_cooldown > 0: dash_cooldown = max(0,dash_cooldown-dt)
            if attack_cooldown > 0: attack_cooldown = max(0,attack_cooldown-dt)
            if attack_time > 0: attack_time = max(0,attack_time-dt)
            if invuln > 0: invuln = max(0,invuln-dt)
            if hilpo_message_time > 0: hilpo_message_time = max(0,hilpo_message_time-dt)
            if scan_time > 0: scan_time = max(0,scan_time-dt)

            for c in crystals[:]:
                if player.colliderect(c):
                    crystals.remove(c)
                    coins += 25
                    add_particles(c.centerx,c.centery,GOLD,24,130)
                    show_message("HILPO: Shard secured. +25 Veil Coins.")
            near_echo = abs(player.centerx-echo_box.centerx)<130 and abs(player.centery-echo_box.centery)<120

            camera_x = clamp(player.centerx-W*.42,0,WORLD_W-W)
            canvas = pygame.Surface((W,H))
            draw_background(canvas)
            draw_platforms(canvas)
            draw_crystals(canvas)
            draw_portal(canvas)
            draw_echo(canvas)
            draw_particles(canvas)
            draw_hollow(canvas)
            draw_hilpo(canvas)
            draw_hud(canvas)

            if near_echo and not dialogue_open:
                text(canvas,"E  /  ACTION: SPEAK TO ECHO",(360,420),LILAC,small)
            if dialogue_open:
                draw_dialogue(canvas,"ECHO",echo_lines[dialogue_index])
            elif hilpo_message_time > 0:
                rounded(canvas,(270,385,420,48),(19,13,35),10,(119,83,160),1)
                text(canvas,hilpo_message,(286,401),PALE,small)
            if scan_time > 0:
                pygame.draw.circle(canvas,(190,148,255),(int(hilpo_x-camera_x),int(hilpo_y)),int(35+(1.2-scan_time)*100),2)
            if won:
                rounded(canvas,(235,185,490,150),(18,12,34),18,(190,145,245),2)
                text(canvas,"THE GATE REMEMBERS",(320,210),GOLD,large)
                text(canvas,"The Awakening — prototype complete",(335,280),PALE,small)
                text(canvas,"Press ESC to return to title",(370,310),LILAC,tiny)

            for name,label in (("left","◀"),("right","▶"),("scan","SCAN"),("action","ACT"),("jump","JUMP"),("attack","ATTACK")):
                draw_button(canvas,label,btn[name],name in pressed)
            draw_button(canvas,"SHOP  [B]",btn["shop"],shop_open)
            if shop_open: draw_shop(canvas)
            if dash_cooldown > 0:
                rounded(canvas,(22,415,110,8),(35,25,50),4)
                pygame.draw.rect(canvas,PURPLE,(22,415,int(110*(1-dash_cooldown/.85)),8),border_radius=4)

        # Integer-ish nearest-neighbor presentation preserves pixel edges
        sw,sh = screen.get_size()
        scale = min(sw/W,sh/H)
        vw,vh = int(W*scale),int(H*scale)
        ox,oy = (sw-vw)//2,(sh-vh)//2
        scaled = pygame.transform.scale(canvas,(max(1,vw),max(1,vh)))
        screen.fill(INK)
        screen.blit(scaled,(ox,oy))
        pygame.display.flip()

    pygame.quit()
    sys.exit()

if __name__ == "__main__":
    main()
