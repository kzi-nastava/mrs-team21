# Brand Assets - Master Repository

This directory contains the **master versions** of all brand assets. All other platforms copy from here.

## Files

- `logo-blue` - Main logo (default color)
- `logo-white` - White version for dark backgrounds
- `logo-black` - Black version for light backgrounds
- `logo-blue-black` - Experimental version

## Distribution

After updating assets here, copy to platforms:

### Angular Frontend

```bash
cp docs/assets/branding/*.svg frontend/src/assets/images/logo
```

<!-- ### Android Mobile
Convert SVG to Vector Drawable:
1. https://svg2vector.com/
2. Upload SVG → Download XML
3. Save to `mobile/app/src/main/res/drawable/ic_logo_[variant].xml`

### Backend (for emails)
```bash
cp docs/assets/branding/logo.svg backend/src/main/resources/static/images/
``` -->

## Git Workflow

Always commit master and copies together:

```bash
git add docs/assets/branding/logo.svg
git add frontend/src/assets/images/logo.svg
git add mobile/app/src/main/res/drawable/ic_logo.xml
git commit -m "Update logo across platforms"
```
