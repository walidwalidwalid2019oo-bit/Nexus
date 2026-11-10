import math
import sys
import pygame

# PURPLE HAZE - phone-first platformer prototype
pygame.init()
pygame.display.set_caption("PURPLE HAZE")
screen = pygame.display.set_mode((960, 540), pygame.RESIZABLE)
clock = pygame.time.Clock()

W, H = 960, 540
PURPLE = (119, 75, 190)
DARK = (17, 12, 35)
LILAC = (205, 170, 255)
WHITE = (245, 238, 255)
GOLD = (255, 220, 130)

world_width = 2300
player = pygame.Rect(100, 330, 34, 48)
spawn = (100, 330)
vx, vy = 0.0, 0.0
on_ground = False
camera_x = 0.0
talk = False
won = False
echo_line = 0
font = pygame.font.Font(None, 30)
small = pygame.font.Font(None, 22)

platforms = [
    pygame.Rect(0, 410, 520, 70),
    pygame.Rect(590, 360, 300, 55),
    pygame.Rect(950, 320, 300, 55),
    pygame.Rect(1320, 390, 330, 55),
    pygame.Rect(1710, 370, 520, 90),
]
crystals = [
    pygame.Rect(470, 365, 22, 30),
    pygame.Rect(1080, 275, 22, 30),
    pygame.Rect(1830, 325, 22, 30),
]
echo_rect = pygame.Rect(1080, 270, 30, 45)
portal = pygame.Rect(2110, 270, 65, 100)

# Pointer state persists between frames, so holding a touch button keeps moving.
active_fingers = {}
mouse_down = False
mouse_pos = (0, 0)
jump_was_down = False

def draw_text(surface, message, x, y, color=WHITE, face=font):
    surface.blit(face.render(message, True, color), (x, y))

def viewport():
    sw, sh = screen.get_size()
    scale = min(sw / W, sh / H)
    vw, vh = int(W * scale), int(H * scale)
    return sw, sh, scale, (sw - vw) // 2, (sh - vh) // 2, vw, vh

def to_game_coords(pos):
    sw, sh, scale, ox, oy, vw, vh = viewport()
    x, y = pos
    return ((x - ox) / scale, (y - oy) / scale)

def controls():
    return {
        "left": pygame.Rect(22, 438, 78, 76),
        "right": pygame.Rect(112, 438, 78, 76),
        "jump": pygame.Rect(850, 438, 88, 76),
        "echo": pygame.Rect(850, 350, 88, 72),
    }

def draw_button(surface, label, rect, pressed=False):
    fill = (112, 78, 160) if pressed else (57, 39, 91)
    pygame.draw.rect(surface, fill, rect, border_radius=15)
    pygame.draw.rect(surface, (177, 139, 235), rect, 2, border_radius=15)
    txt = small.render(label, True, WHITE)
    surface.blit(txt, txt.get_rect(center=rect.center))

def resolve_horizontal(rect, dx):
    rect.x += int(dx)
    for solid in platforms:
        if rect.colliderect(solid):
            if dx > 0:
                rect.right = solid.left
            elif dx < 0:
                rect.left = solid.right

def resolve_vertical(rect, dy):
    global vy, on_ground
    rect.y += int(dy)
    on_ground = False
    for solid in platforms:
        if rect.colliderect(solid):
            if dy > 0:
                rect.bottom = solid.top
                on_ground = True
            elif dy < 0:
                rect.top = solid.bottom
            vy = 0.0
    if rect.bottom >= 520:
        rect.bottom = 520
        vy = 0.0
        on_ground = True

def echo_speak():
    global talk, echo_line
    talk = not talk
    if talk:
        echo_line = (echo_line + 1) % 3

echo_messages = [
    "The crystals remember the way. Follow their light.",
    "Some paths only appear when you keep moving.",
    "The portal is quiet. Three lights will wake it.",
]

def draw_world(canvas, camera):
    # Soft night-sky gradient
    for y in range(H):
        t = y / H
        color = (int(18 + 18*t), int(12 + 8*t), int(43 + 35*t))
        pygame.draw.line(canvas, color, (0, y), (W, y))

    # Stars drift at a slower speed than the world.
    for i in range(55):
        x = int((i * 197 - camera * 0.22) % W)
        y = (i * 83) % 300
        pygame.draw.circle(canvas, (145, 122, 185), (x, y), 1)

    pygame.draw.circle(canvas, (83, 55, 132), (760, 110), 62)
    pygame.draw.circle(canvas, (145, 104, 190), (740, 90), 10)

    # Platforms
    for p in platforms:
        r = pygame.Rect(int(p.x-camera), p.y, p.width, p.height)
        pygame.draw.rect(canvas, (49, 31, 78), r, border_radius=12)
        pygame.draw.rect(canvas, (137, 93, 185), (r.x, r.y, r.width, 8), border_radius=5)
        # Tiny glowing stones along the island edge
        for gx in range(r.left + 18, r.right - 8, 70):
            pygame.draw.circle(canvas, (94, 66, 132), (gx, r.bottom - 12), 3)

    # Crystals
    for crystal in crystals:
        cx = crystal.centerx - camera
        pygame.draw.polygon(canvas, GOLD, [
            (cx, crystal.top), (crystal.right-camera, crystal.centery),
            (cx, crystal.bottom), (crystal.left-camera, crystal.centery)
        ])
        pygame.draw.polygon(canvas, WHITE, [
            (cx, crystal.top+5), (cx+4, crystal.centery), (cx, crystal.bottom-5)
        ])

    # ECHO guide
    er = pygame.Rect(echo_rect.x-camera, echo_rect.y, echo_rect.width, echo_rect.height)
    pulse = int(3 * math.sin(pygame.time.get_ticks() / 300))
    pygame.draw.circle(canvas, LILAC, (er.centerx, er.y + 7), 15 + pulse)
    pygame.draw.rect(canvas, (105, 77, 155), (er.x, er.y+18, er.width, 27), border_radius=8)
    draw_text(canvas, "ECHO", er.x-5, er.y-25, LILAC, small)

    # Portal
    pr = pygame.Rect(portal.x-camera, portal.y, portal.width, portal.height)
    pygame.draw.ellipse(canvas, (102, 63, 174), pr, 7)
    pygame.draw.ellipse(canvas, LILAC, pr.inflate(-18, -15), 3)

    # Player
    px = int(player.x-camera)
    pygame.draw.ellipse(canvas, (91, 65, 130), (px-5, player.bottom-4, 44, 10))
    pygame.draw.rect(canvas, (225, 205, 255), (px, player.y, player.width, player.height), border_radius=8)
    pygame.draw.rect(canvas, (145, 111, 194), (px+6, player.y+9, 7, 7), border_radius=3)
    pygame.draw.rect(canvas, (145, 111, 194), (px+21, player.y+9, 7, 7), border_radius=3)

def main():
    global vx, vy, on_ground, camera_x, talk, won, mouse_down, mouse_pos
    running = True
    while running:
        dt = min(clock.tick(60) / 1000.0, 0.025)
        sw, sh, scale, ox, oy, vw, vh = viewport()
        keys = pygame.key.get_pressed()
        btn = controls()
        echo_pressed = False

        for event in pygame.event.get():
            if event.type == pygame.QUIT:
                running = False
            elif event.type == pygame.VIDEORESIZE:
                screen = pygame.display.set_mode((max(480, event.w), max(270, event.h)), pygame.RESIZABLE)
            elif event.type == pygame.KEYDOWN:
                if event.key == pygame.K_ESCAPE:
                    running = False
                elif event.key == pygame.K_e:
                    echo_pressed = True
                elif event.key in (pygame.K_SPACE, pygame.K_UP, pygame.K_w) and on_ground:
                    vy = -510
                    on_ground = False
            elif event.type == pygame.MOUSEBUTTONDOWN and event.button == 1:
                mouse_down = True
                mouse_pos = event.pos
            elif event.type == pygame.MOUSEBUTTONUP and event.button == 1:
                mouse_down = False
            elif event.type == pygame.MOUSEMOTION:
                mouse_pos = event.pos
            elif event.type == pygame.FINGERDOWN:
                active_fingers[event.finger_id] = (event.x * sw, event.y * sh)
            elif event.type == pygame.FINGERMOTION:
                active_fingers[event.finger_id] = (event.x * sw, event.y * sh)
            elif event.type == pygame.FINGERUP:
                active_fingers.pop(event.finger_id, None)

        pointer_positions = list(active_fingers.values())
        if mouse_down:
            pointer_positions.append(mouse_pos)
        pressed = set()
        for point in pointer_positions:
            gx, gy = to_game_coords(point)
            for name, rect in btn.items():
                if rect.collidepoint(gx, gy):
                    pressed.add(name)

        move_left = keys[pygame.K_LEFT] or keys[pygame.K_a] or "left" in pressed
        move_right = keys[pygame.K_RIGHT] or keys[pygame.K_d] or "right" in pressed
        jump_down = keys[pygame.K_SPACE] or keys[pygame.K_UP] or keys[pygame.K_w] or "jump" in pressed
        if jump_down and not jump_was_down and on_ground:
            vy = -510
            on_ground = False
        jump_was_down = jump_down
        echo_pressed = echo_pressed or "echo" in pressed

        vx = (-230 if move_left else 0) + (230 if move_right else 0)
        resolve_horizontal(player, vx * dt)
        vy = min(vy + 1250 * dt, 800)
        resolve_vertical(player, vy * dt)

        # Simple respawn if the player falls off the world.
        if player.top > H + 100:
            player.topleft = spawn
            vy = 0
            camera_x = 0

        near_echo = abs(player.centerx - echo_rect.centerx) < 115 and abs(player.centery - echo_rect.centery) < 100
        if echo_pressed and near_echo:
            echo_speak()

        # Collect crystals
        crystals[:] = [c for c in crystals if not player.colliderect(c)]
        if len(crystals) == 0 and player.colliderect(portal):
            won = True

        camera_x = max(0, min(world_width-W, player.centerx-W/2))
        canvas = pygame.Surface((W, H))
        draw_world(canvas, camera_x)

        collected = 3 - len(crystals)
        draw_text(canvas, f"CRYSTALS: {collected}/3", 22, 18)
        draw_text(canvas, "MISSION: Find 3 crystals and reach the portal", 22, 48, LILAC, small)

        # Mini-map and player marker
        pygame.draw.rect(canvas, (38, 25, 62), (790, 18, 148, 28), border_radius=8)
        for crystal in crystals:
            mx = 794 + int(crystal.centerx / world_width * 138)
            pygame.draw.circle(canvas, GOLD, (mx, 32), 3)
        marker = 794 + int(player.centerx / world_width * 138)
        pygame.draw.circle(canvas, WHITE, (marker, 32), 5)

        if near_echo:
            draw_text(canvas, "Press E / ECHO to talk", 360, 395, LILAC, small)
        if talk:
            pygame.draw.rect(canvas, (28, 18, 48), (190, 330, 580, 82), border_radius=12)
            pygame.draw.rect(canvas, LILAC, (190, 330, 580, 82), 2, border_radius=12)
            draw_text(canvas, "ECHO: " + echo_messages[echo_line], 210, 360, WHITE, small)
        if won:
            pygame.draw.rect(canvas, (25, 15, 48), (250, 205, 460, 100), border_radius=16)
            draw_text(canvas, "PORTAL RESTORED", 370, 225, GOLD)
            draw_text(canvas, "The first chapter is complete.", 350, 263, WHITE, small)

        for name, label in (("left", "◀"), ("right", "▶"), ("echo", "ECHO"), ("jump", "JUMP")):
            draw_button(canvas, label, btn[name], name in pressed)

        scaled = pygame.transform.smoothscale(canvas, (vw, vh))
        screen.fill((8, 6, 18))
        screen.blit(scaled, (ox, oy))
        pygame.display.flip()

    pygame.quit()
    sys.exit()

if __name__ == "__main__":
    main()
