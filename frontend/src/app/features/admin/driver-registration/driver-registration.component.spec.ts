import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { DriverRegistrationComponent } from './driver-registration.component';
import { DriverRegistrationService } from '../services/driver-registration.service';

describe('DriverRegistrationComponent', () => {
  let component: DriverRegistrationComponent;
  let driverRegistrationService: jasmine.SpyObj<DriverRegistrationService>;

  beforeEach(async () => {
    driverRegistrationService = jasmine.createSpyObj<DriverRegistrationService>(
      'DriverRegistrationService',
      ['registerDriver', 'mapCategoryToTypeId', 'uploadProfilePicture'],
    );

    driverRegistrationService.mapCategoryToTypeId.and.returnValue(2);
    driverRegistrationService.registerDriver.and.returnValue(of({ id: 1 } as any));
    driverRegistrationService.uploadProfilePicture.and.returnValue(of({ url: '/api/uploads/profile/temp/test.jpg' }));

    await TestBed.configureTestingModule({
      imports: [DriverRegistrationComponent],
      providers: [
        provideRouter([]),
        {
          provide: DriverRegistrationService,
          useValue: driverRegistrationService,
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(DriverRegistrationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should stay on step 1 when driver form is invalid', () => {
    component.nextStep();

    expect(component.currentStep).toBe(1);
    expect(component.driverSubmitted).toBe(true);
  });

  it('should not submit when vehicle form is invalid', () => {
    component.driverForm.patchValue({
      firstName: 'Marko',
      lastName: 'Markovic',
      email: 'marko@example.com',
      countryCode: '+381',
      phone: '641234567',
      address: 'Bulevar Oslobodjenja 1',
    });

    component.onSubmit();

    expect(driverRegistrationService.registerDriver).not.toHaveBeenCalled();
  });

  it('should send driver and vehicle payload when forms are valid', () => {
    component.driverForm.patchValue({
      firstName: 'Marko',
      lastName: 'Markovic',
      email: 'marko@example.com',
      countryCode: '+381',
      phone: '641234567',
      address: 'Bulevar Oslobodjenja 1',
    });

    component.vehicleForm.patchValue({
      model: 'Skoda Superb',
      category: 'Luxury',
      licensePlate: 'ns-123-ab',
      seats: 5,
      babySeats: true,
      petFriendly: false,
    });

    component.onSubmit();

    expect(driverRegistrationService.mapCategoryToTypeId).toHaveBeenCalledWith('Luxury');
    expect(driverRegistrationService.registerDriver).toHaveBeenCalledWith({
      name: 'Marko',
      surname: 'Markovic',
      email: 'marko@example.com',
      phone: '+381641234567',
      address: 'Bulevar Oslobodjenja 1',
      profilePictureUrl: null,
      vehicleTypeId: 2,
      vehicleModel: 'Skoda Superb',
      vehicleLicensePlate: 'NS-123-AB',
      vehicleNumSeats: 5,
      vehicleBabyFriendly: true,
      vehiclePetFriendly: false,
    });
    expect(component.showSuccessMessage).toBe(false);
    expect(component.isSubmitting).toBe(false);
  });
});