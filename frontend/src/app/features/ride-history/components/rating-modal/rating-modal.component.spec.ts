import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

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

  it('should not submit when ratings are missing', () => {
    component.onSubmit();

    expect(component.ratingForm.get('driverRating')?.touched).toBe(true);
    expect(component.ratingForm.get('vehicleRating')?.touched).toBe(true);
    expect(reviewService.submitReview).not.toHaveBeenCalled();
  });

  it('should submit driver and vehicle ratings with comment', () => {
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
});