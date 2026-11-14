import os
import sys
from pathlib import Path
from PIL import Image, ImageChops

REPO_ROOT = Path(__file__).resolve().parent.parent

modules = {
    "design": (
        REPO_ROOT / "docs" / "audit" / "batch-11" / "old_snapshots" / "design",
        REPO_ROOT / "design" / "src" / "test" / "snapshots" / "images"
    ),
    "app": (
        REPO_ROOT / "docs" / "audit" / "batch-11" / "old_snapshots" / "app",
        REPO_ROOT / "app" / "src" / "test" / "snapshots" / "images"
    )
}

output_html = REPO_ROOT / "docs" / "audit" / "batch-11" / "contact-sheet.html"

results = []

for mod_name, (old_dir, new_dir) in modules.items():
    if not old_dir.exists() or not new_dir.exists():
        print(f"Skipping {mod_name}: directories not found")
        continue

    for new_file in sorted(new_dir.glob("*.png")):
        filename = new_file.name
        old_file = old_dir / filename
        
        has_old = old_file.exists()
        diff_pct = 0.0
        dims_match = False
        old_dims = (0, 0)
        new_dims = (0, 0)
        
        with Image.open(new_file) as img_new:
            new_dims = img_new.size
            
        if has_old:
            with Image.open(old_file) as img_old:
                old_dims = img_old.size
                
            if old_dims == new_dims:
                dims_match = True
                with Image.open(old_file).convert("RGBA") as im1, Image.open(new_file).convert("RGBA") as im2:
                    diff = ImageChops.difference(im1, im2)
                    diff_data = diff.getdata()
                    total_pixels = len(diff_data)
                    changed_pixels = sum(1 for p in diff_data if max(p[:3]) > 2) # ignore tiny 1-2 channel jitter
                    diff_pct = (changed_pixels / total_pixels) * 100.0
            else:
                dims_match = False
                diff_pct = 100.0
                
        results.append({
            "module": mod_name,
            "filename": filename,
            "has_old": has_old,
            "dims_match": dims_match,
            "old_dims": old_dims,
            "new_dims": new_dims,
            "diff_pct": diff_pct,
            "old_rel": os.path.relpath(old_file, output_html.parent).replace("\\", "/"),
            "new_rel": os.path.relpath(new_file, output_html.parent).replace("\\", "/")
        })

print(f"Compared {len(results)} snapshots.")
total = len(results)
identical = sum(1 for r in results if r["diff_pct"] == 0.0)
under_1pct = sum(1 for r in results if 0.0 < r["diff_pct"] <= 1.0)
over_1pct = sum(1 for r in results if r["diff_pct"] > 1.0)
print(f"Identical: {identical}, <=1% diff: {under_1pct}, >1% diff: {over_1pct}")

html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>Batch 11 Golden Snapshots Contact Sheet (Paparazzi vs Roborazzi)</title>
<style>
  body {{ font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; background: #0f1115; color: #e6edf3; margin: 0; padding: 20px; }}
  h1, h2, h3 {{ color: #ffffff; }}
  .summary-card {{ background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 16px 24px; margin-bottom: 24px; display: flex; gap: 32px; }}
  .stat {{ display: flex; flex-direction: column; }}
  .stat-value {{ font-size: 28px; font-weight: bold; color: #58a6ff; }}
  .stat-label {{ font-size: 13px; color: #8b949e; text-transform: uppercase; letter-spacing: 0.5px; }}
  .grid {{ display: grid; grid-template-columns: repeat(auto-fill, minmax(480px, 1fr)); gap: 24px; }}
  .card {{ background: #161b22; border: 1px solid #30363d; border-radius: 8px; overflow: hidden; display: flex; flex-direction: column; }}
  .card-header {{ padding: 12px 16px; background: #21262d; border-bottom: 1px solid #30363d; font-size: 13px; font-weight: 600; display: flex; justify-content: space-between; align-items: center; }}
  .card-header .badge {{ padding: 2px 8px; border-radius: 12px; font-size: 11px; }}
  .badge-green {{ background: #238636; color: white; }}
  .badge-yellow {{ background: #9e6a03; color: white; }}
  .badge-blue {{ background: #1f6feb; color: white; }}
  .comparison-view {{ display: flex; gap: 8px; padding: 12px; justify-content: center; background: #090d13; }}
  .img-col {{ flex: 1; text-align: center; max-width: 50%; }}
  .img-col img {{ max-width: 100%; height: auto; border: 1px solid #30363d; border-radius: 4px; display: block; margin: 0 auto; background: #1a1e24; }}
  .img-label {{ font-size: 11px; color: #8b949e; margin-top: 6px; font-family: monospace; }}
</style>
</head>
<body>
  <h1>Batch 11 Golden Snapshots Contact Sheet</h1>
  <p style="color: #8b949e;">Visual comparison between old Paparazzi 1.3.5 / LayoutLib snapshots and new Roborazzi 1.79.0 / Robolectric Native Graphics snapshots.</p>

  <div class="summary-card">
    <div class="stat"><span class="stat-value">{total}</span><span class="stat-label">Total Snapshots</span></div>
    <div class="stat"><span class="stat-value">{sum(1 for r in results if r['has_old'])}/{total}</span><span class="stat-label">Old Snapshots Paired</span></div>
    <div class="stat"><span class="stat-value">Roborazzi 1.79.0</span><span class="stat-label">New Engine</span></div>
    <div class="stat"><span class="stat-value">Robolectric Native Graphics</span><span class="stat-label">Graphics Mode</span></div>
  </div>

  <h2>All Snapshots ({total})</h2>
  <div class="grid">
"""

for r in results:
    old_dim_str = f"{r['old_dims'][0]}x{r['old_dims'][1]}" if r['has_old'] else "N/A"
    new_dim_str = f"{r['new_dims'][0]}x{r['new_dims'][1]}"
    status_label = "Paparazzi -> Roborazzi"
    
    html_content += f"""
    <div class="card">
      <div class="card-header">
        <span style="font-family: monospace; word-break: break-all;">{r['filename']}</span>
        <span class="badge badge-blue">{status_label}</span>
      </div>
      <div class="comparison-view">
        <div class="img-col">
          <img src="{r['old_rel']}" alt="Paparazzi" loading="lazy">
          <div class="img-label">Paparazzi LayoutLib ({old_dim_str})</div>
        </div>
        <div class="img-col">
          <img src="{r['new_rel']}" alt="Roborazzi" loading="lazy">
          <div class="img-label">Roborazzi Native ({new_dim_str})</div>
        </div>
      </div>
    </div>
    """

html_content += """
  </div>
</body>
</html>
"""

output_html.write_text(html_content, encoding="utf-8")
print(f"Wrote contact sheet to {output_html}")
