import { Component, Input, Output, EventEmitter, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

/**
 * Unified profile photo upload component for both registration and profile views.
 * Shows current/default photo with click-to-upload overlay.
 * Automatically crops to 1:1 aspect ratio (center crop).
 * 
 * Backend Integration:
 * - Emits File object through fileSelected event
 * - Parent handles upload to backend and receives URL
 * - Backend stores URL in User.profilePictureUrl field
 * 
 * Usage:
 * ```html
 * <app-profile-photo-upload
 *   [currentPhotoUrl]="user?.avatarUrl"
 *   (fileSelected)="onPhotoSelected($event)">
 * </app-profile-photo-upload>
 * ```
 */
@Component({
  selector: 'app-profile-photo-upload',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './profile-photo-upload.component.html',
  styleUrl: './profile-photo-upload.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ProfilePhotoUploadComponent),
      multi: true
    }
  ]
})
export class ProfilePhotoUploadComponent implements ControlValueAccessor {
  /**
   * Current photo URL to display (null shows default avatar)
   */
  @Input() currentPhotoUrl: string | null | undefined = null;

  /**
   * Size of the avatar (default 120px)
   */
  @Input() size: number = 120;

  /**
   * Maximum file size in bytes (default 5MB)
   */
  @Input() maxFileSize: number = 5 * 1024 * 1024;

  /**
   * Accepted file types
   */
  @Input() acceptedTypes: string = 'image/*';

  /**
   * Emits when a file is selected and processed
   */
  @Output() fileSelected = new EventEmitter<File>();

  // Internal state
  previewUrl: string | null = null;
  selectedFile: File | null = null;
  errorMessage: string | null = null;
  isHovering = false;

  // ControlValueAccessor properties
  private onChange: (value: File | null) => void = () => {};
  private onTouched: () => void = () => {};
  disabled = false;

  /**
   * Handle file selection and auto-crop to 1:1 center square
   */
  async onFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    if (!input.files || !input.files[0]) {
      return;
    }

    const file = input.files[0];
    this.errorMessage = null;

    // Validate file size
    if (file.size > this.maxFileSize) {
      this.errorMessage = `File size must be less than ${this.maxFileSize / (1024 * 1024)}MB`;
      return;
    }

    // Validate file type
    if (!file.type.startsWith('image/')) {
      this.errorMessage = 'Please select a valid image file';
      return;
    }

    try {
      // Crop image to 1:1 center square
      const croppedFile = await this.cropImageToSquare(file);
      this.selectedFile = croppedFile;

      // Create preview URL
      this.previewUrl = URL.createObjectURL(croppedFile);

      // Update current photo URL to show the new photo immediately
      this.currentPhotoUrl = this.previewUrl;

      // Emit file selection
      this.fileSelected.emit(croppedFile);
      this.onChange(croppedFile);
      this.onTouched();

    } catch (error) {
      console.error('Error processing image:', error);
      this.errorMessage = 'Error processing image. Please try again.';
      this.previewUrl = null;
      this.selectedFile = null;
    }
  }

  /**
   * Crop image to 1:1 aspect ratio (center crop)
   * TODO: Add advanced cropping UI for user to select crop area
   * TODO: Add image rotation support
   * TODO: Add image compression based on file size
   */
  private async cropImageToSquare(file: File): Promise<File> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');

      if (!ctx) {
        reject(new Error('Could not get canvas context'));
        return;
      }

      img.onload = () => {
        // Calculate center square dimensions
        const size = Math.min(img.width, img.height);
        const x = (img.width - size) / 2;
        const y = (img.height - size) / 2;

        // Set canvas to square
        canvas.width = size;
        canvas.height = size;

        // Draw cropped image
        ctx.drawImage(img, x, y, size, size, 0, 0, size, size);

        // Convert to blob
        canvas.toBlob(
          (blob) => {
            if (blob) {
              // Create new file with original name
              const croppedFile = new File([blob], file.name, {
                type: file.type,
                lastModified: Date.now(),
              });
              resolve(croppedFile);
            } else {
              reject(new Error('Failed to create blob'));
            }
          },
          file.type,
          0.95 // Quality (TODO: Make configurable)
        );

        // Clean up
        URL.revokeObjectURL(img.src);
      };

      img.onerror = () => {
        reject(new Error('Failed to load image'));
        URL.revokeObjectURL(img.src);
      };

      // Load image
      img.src = URL.createObjectURL(file);
    });
  }

  /**
   * Trigger file input click
   */
  triggerFileInput(fileInput: HTMLInputElement): void {
    if (!this.disabled) {
      fileInput.click();
    }
  }

  /**
   * Handle image load error - fallback to default avatar
   */
  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = 'images/default-avatar.svg';
  }

  /**
   * Get the display URL for the avatar
   */
  get displayUrl(): string {
    return this.currentPhotoUrl || 'images/default-avatar.svg';
  }

  // ControlValueAccessor implementation
  writeValue(value: File | null): void {
    this.selectedFile = value;
    // Don't create preview from writeValue as we can't convert File to URL reliably
  }

  registerOnChange(fn: (value: File | null) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  /**
   * Cleanup on component destroy
   */
  ngOnDestroy(): void {
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
    }
  }
}
