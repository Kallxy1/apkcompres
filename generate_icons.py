import os
from PIL import Image, ImageDraw

def create_app_icon(size):
    # Create image with transparent background
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Coordinates
    center = size / 2
    radius = size * 0.4
    
    # Draw minimalist dark grey/black circle background
    draw.ellipse([center - radius, center - radius, center + radius, center + radius], fill=(18, 18, 18, 255))
    
    # Draw Glowing Neon Green Status Ring (thick circle)
    ring_width = max(2, int(size * 0.04))
    draw.ellipse([center - radius + ring_width, center - radius + ring_width, 
                  center + radius - ring_width, center + radius - ring_width], 
                 outline=(37, 211, 102, 255), width=ring_width)
    
    # Draw a minimalist glowing play triangle/lightning bolt in the center
    # Coordinates for triangle
    offset_x = size * 0.05
    p1 = (center - size * 0.1 + offset_x, center - size * 0.18)
    p2 = (center + size * 0.18 + offset_x, center)
    p3 = (center - size * 0.1 + offset_x, center + size * 0.18)
    draw.polygon([p1, p2, p3], fill=(37, 211, 102, 255))
    
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
    img = create_app_icon(size)
    img.save(os.path.join(dir_path, "ic_launcher.png"), "PNG")
    img.save(os.path.join(dir_path, "ic_launcher_round.png"), "PNG")

print("All icons successfully generated using PIL!")
