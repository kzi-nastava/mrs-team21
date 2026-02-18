import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { RatingModalComponent, RideForRating } from './rating-modal.component';
import { ReviewResponse, ReviewService } from '../../services/review.service';
import { ToastService } from '../../../../shared/services/toast.service';

describe('RatingModalComponent', () => {
  let component: RatingModalComponent;
  let reviewService: jasmine.SpyObj<ReviewService>;
  let toastService: jasmine.SpyObj<ToastService>;

  const ride: RideForRating = {
    id: '42',
    driverName: 'Petar Petrovic',
    origin: 'Promenada',
    destination: 'Futoska pijaca',
    vehicleModel: 'Toyota Corolla',
    vehicleLicensePlate: 'NS-321-BA',
  };

  const reviewResponse: ReviewResponse = {
    id: 10,
    rideId: 42,
    passengerId: 7,
    passengerName: 'Milos',
    ratingDriver: 5,
    ratingVehicle: 4,
    comment: 'Sve pohvale.',
    createdAt: '2026-02-18T10:00:00Z',
  };

  beforeEach(async () => {
    reviewService = jasmine.createSpyObj<ReviewService>('ReviewService', [
      'submitReview',
      'getRatingStatus',
      'getRideReviews',
    ]);
    toastService = jasmine.createSpyObj<ToastService>('ToastService', ['success', 'error']);

    reviewService.submitReview.and.returnValue(of(reviewResponse));

    await TestBed.configureTestingModule({
      imports: [RatingModalComponent],
      providers: [
        { provide: ReviewService, useValue: reviewService },
        { provide: ToastService, useValue: toastService },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(RatingModalComponent);
    fixture.componentRef.setInput('ride', ride);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // Negative test cases
  it('should not submit when both ratings are not selected', () => {
    component.onSubmit();

    expect(component.ratingForm.get('driverRating')?.touched).toBe(true);
    expect(component.ratingForm.get('vehicleRating')?.touched).toBe(true);
    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  it('should not submit when only driver rating is not selected', () => {
    component.ratingForm.patchValue({ vehicleRating: 4 });
    component.onSubmit();

    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  it('should not submit when only vehicle rating is not selected', () => {
    component.ratingForm.patchValue({ driverRating: 4 });
    component.onSubmit();

    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  it('should not submit when comment exceeds max length (501 characters)', () => {
    component.ratingForm.setValue({
      driverRating: 4,
      vehicleRating: 4,
      comment: 'a'.repeat(501),
    });
    component.onSubmit();

    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  // Positive test cases
  it('should submit driver and vehicle ratings with comment and verify sent data', () => {
    const submittedEmit = spyOn(component.submitted, 'emit');
    const closeEmit = spyOn(component.close, 'emit');

    component.ratingForm.setValue({
      driverRating: 5,
      vehicleRating: 4,
      comment: 'Sve pohvale.',
    });

    component.onSubmit();

    expect(reviewService.submitReview).toHaveBeenCalledWith('42', {
      ratingDriver: 5,
      ratingVehicle: 4,
      comment: 'Sve pohvale.',
    });
    expect(toastService.success).toHaveBeenCalled();
    expect(submittedEmit).toHaveBeenCalledWith(reviewResponse);
    expect(closeEmit).toHaveBeenCalled();
    expect(component.isSubmitting()).toBe(false);
  });

  it('should submit without comment and send comment as undefined', () => {
    const submittedEmit = spyOn(component.submitted, 'emit');
    const closeEmit = spyOn(component.close, 'emit');
    const responseNoComment: ReviewResponse = { ...reviewResponse, comment: '' };
    reviewService.submitReview.and.returnValue(of(responseNoComment));

    component.ratingForm.setValue({
      driverRating: 3,
      vehicleRating: 3,
      comment: '',
    });

    component.onSubmit();

    expect(reviewService.submitReview).toHaveBeenCalledWith('42', {
      ratingDriver: 3,
      ratingVehicle: 3,
      comment: undefined,
    });
    expect(submittedEmit).toHaveBeenCalledWith(responseNoComment);
    expect(closeEmit).toHaveBeenCalled();
  });

  // Boundary test cases — ratings valid range [1, 5], comment max 500
  it('should accept ratings at lower boundary (1)', () => {
    const submittedEmit = spyOn(component.submitted, 'emit');
    const responseMin: ReviewResponse = { ...reviewResponse, ratingDriver: 1, ratingVehicle: 1 };
    reviewService.submitReview.and.returnValue(of(responseMin));

    component.ratingForm.setValue({ driverRating: 1, vehicleRating: 1, comment: '' });
    component.onSubmit();

    expect(reviewService.submitReview).toHaveBeenCalledWith('42', {
      ratingDriver: 1,
      ratingVehicle: 1,
      comment: undefined,
    });
    expect(submittedEmit).toHaveBeenCalledWith(responseMin);
  });

  it('should accept ratings at upper boundary (5)', () => {
    const submittedEmit = spyOn(component.submitted, 'emit');
    const responseMax: ReviewResponse = { ...reviewResponse, ratingDriver: 5, ratingVehicle: 5 };
    reviewService.submitReview.and.returnValue(of(responseMax));

    component.ratingForm.setValue({ driverRating: 5, vehicleRating: 5, comment: '' });
    component.onSubmit();

    expect(reviewService.submitReview).toHaveBeenCalledWith('42', {
      ratingDriver: 5,
      ratingVehicle: 5,
      comment: undefined,
    });
    expect(submittedEmit).toHaveBeenCalledWith(responseMax);
  });

  it('should reject driver rating just below lower boundary (0)', () => {
    component.ratingForm.setValue({ driverRating: 0, vehicleRating: 3, comment: '' });
    component.onSubmit();

    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  it('should reject driver rating just above upper boundary (6)', () => {
    component.ratingForm.setValue({ driverRating: 6, vehicleRating: 3, comment: '' });
    component.onSubmit();

    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  it('should reject vehicle rating just below lower boundary (0)', () => {
    component.ratingForm.setValue({ driverRating: 3, vehicleRating: 0, comment: '' });
    component.onSubmit();

    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  it('should reject vehicle rating just above upper boundary (6)', () => {
    component.ratingForm.setValue({ driverRating: 3, vehicleRating: 6, comment: '' });
    component.onSubmit();

    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  it('should accept comment at max length (500 characters)', () => {
    const longComment = 'a'.repeat(500);
    reviewService.submitReview.and.returnValue(of({ ...reviewResponse, comment: longComment }));

    component.ratingForm.setValue({ driverRating: 4, vehicleRating: 4, comment: longComment });
    component.onSubmit();

    expect(reviewService.submitReview).toHaveBeenCalledWith('42', {
      ratingDriver: 4,
      ratingVehicle: 4,
      comment: longComment,
    });
  });

  // Exceptional test cases
  it('should show error toast and not close when server returns an error', () => {
    spyOn(console, 'error');
    const submittedEmit = spyOn(component.submitted, 'emit');
    const closeEmit = spyOn(component.close, 'emit');
    reviewService.submitReview.and.returnValue(
      throwError(() => ({ error: { message: 'Server error' } })),
    );

    component.ratingForm.setValue({ driverRating: 5, vehicleRating: 4, comment: '' });
    component.onSubmit();

    expect(reviewService.submitReview).toHaveBeenCalled();
    expect(toastService.error).toHaveBeenCalledWith('Server error');
    expect(submittedEmit).not.toHaveBeenCalled();
    expect(closeEmit).not.toHaveBeenCalled();
    expect(component.isSubmitting()).toBe(false);
  });

  it('should not submit when a submission is already in progress', () => {
    component.isSubmitting.set(true);
    component.ratingForm.setValue({ driverRating: 5, vehicleRating: 4, comment: '' });

    component.onSubmit();

    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });
});