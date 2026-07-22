import os
from PIL import Image, ImageDraw

def process_and_resize(source_path, dest_dir, size):
    os.makedirs(dest_dir, exist_ok=True)
    
    # Load raw generated icon
    img = Image.open(source_path).convert("RGBA")
    
    # Resize to target size
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    
    # Save standard launcher icon
    resized.save(os.path.join(dest_dir, "ic_launcher.png"), "PNG")
    
    # Create circular mask for round launcher icon
    mask = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size, size), fill=255)
    
    round_img = Image.new('RGBA', (size, size), (0,0,0,0))
    round_img.paste(resized, (0, 0), mask=mask)
    
    # Save round launcher icon
    round_img.save(os.path.join(dest_dir, "ic_launcher_round.png"), "PNG")

# Paths for Android mipmap folders
base_path = "app/src/main/res"
folders = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

source_image = "app_logo_raw.png"
if os.path.exists(source_image):
    for folder, size in folders.items():
        process_and_resize(source_image, os.path.join(base_path, folder), size)
    print("Successfully processed and resized AI-generated icon for all resolutions!")
else:
    print("Error: Source AI-generated image not found.")
