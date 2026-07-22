import os
from PIL import Image, ImageDraw, ImageFilter

def create_super_cool_icon(size):
    # Create image with transparent background
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    
    # Create background canvas for glowing effects
    bg = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw_bg = ImageDraw.Draw(bg)
    
    center = size / 2
    radius = size * 0.44
    
    # 1. Dark radial gradient-like background circle
    for r in range(int(radius), 0, -2):
        alpha = int(255 * (1.0 - (r / radius) * 0.3))
        draw_bg.ellipse([center - r, center - r, center + r, center + r], 
                        fill=(10, 24, 15, alpha)) # Dark neon-green tint
                        
    # Draw pure dark black center circle
    inner_r = radius * 0.95
    draw_bg.ellipse([center - inner_r, center - inner_r, center + inner_r, center + inner_r], 
                    fill=(10, 10, 10, 255))
    
    # 2. Glowing HUD elements (Status Ring with modern dashes)
    ring_r = radius * 0.85
    ring_width = max(2, int(size * 0.04))
    
    # Draw glowing aura for the ring
    glow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw_glow = ImageDraw.Draw(glow)
    draw_glow.ellipse([center - ring_r, center - ring_r, center + ring_r, center + ring_r], 
                      outline=(37, 211, 102, 100), width=ring_width + 4)
    glow = glow.filter(ImageFilter.GaussianBlur(ring_width / 2))
    img = Image.alpha_composite(img, glow)
    
    # Merge bg
    img = Image.alpha_composite(img, bg)
    
    # Draw crisp outer segmented rings
    draw_img = ImageDraw.Draw(img)
    draw_img.ellipse([center - ring_r, center - ring_r, center + ring_r, center + ring_r], 
                     outline=(37, 211, 102, 255), width=ring_width)
    
    # Segmented marks
    mark_len = size * 0.08
    for i in range(8):
        # We can just draw minimalist cross lines across the ring
        draw_img.line([center, 10, center, 10 + mark_len], fill=(37, 211, 102, 255), width=2)
        draw_img.line([center, size - 10, center, size - 10 - mark_len], fill=(37, 211, 102, 255), width=2)
        draw_img.line([10, center, 10 + mark_len, center], fill=(37, 211, 102, 255), width=2)
        draw_img.line([size - 10, center, size - 10 - mark_len, center], fill=(37, 211, 102, 255), width=2)

    # 3. Crystal Futuristic Triangle in Center
    offset_x = size * 0.04
    p1 = (center - size * 0.08 + offset_x, center - size * 0.16)
    p2 = (center + size * 0.16 + offset_x, center)
    p3 = (center - size * 0.08 + offset_x, center + size * 0.16)
    
    # Draw triangle glow
    tglow = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw_tglow = ImageDraw.Draw(tglow)
    draw_tglow.polygon([p1, p2, p3], fill=(37, 211, 102, 150))
    tglow = tglow.filter(ImageFilter.GaussianBlur(ring_width))
    img = Image.alpha_composite(img, tglow)
    
    # Crisp inner triangle
    draw_img.polygon([p1, p2, p3], fill=(37, 211, 102, 255))
    
    return img

# Paths for Android mipmap folders
base_path = "app/src/main/res"
folders = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

for folder, size in folders.items():
    dir_path = os.path.join(base_path, folder)
    os.makedirs(dir_path, exist_ok=True)
    img = create_super_cool_icon(size)
    img.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")
    img.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

print("All premium icons successfully generated!")
