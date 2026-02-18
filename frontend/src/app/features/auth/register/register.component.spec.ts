import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { RegisterComponent } from './register.component';
import { RegisterService } from '../services/register.service';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let router: Router;
  let registerService: jasmine.SpyObj<RegisterService>;

  beforeEach(async () => {
    registerService = jasmine.createSpyObj<RegisterService>('RegisterService', ['register']);
    registerService.register.and.returnValue(of({ id: 1 }));

    await TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        {
          provide: RegisterService,
          useValue: registerService,
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('should not send registration request when form is invalid', () => {
    component.onSubmit();

    expect(registerService.register).not.toHaveBeenCalled();
  });

  it('should send user registration payload and navigate to login', () => {
    const navigateSpy = spyOn(router, 'navigate').and.returnValue(Promise.resolve(true));
    const photo = new File(['avatar'], 'avatar.png', { type: 'image/png' });

    component.onPhotoSelected(photo);
    component.registerForm.patchValue({
      firstName: 'Milos',
      lastName: 'Milosevic',
      email: 'milos@example.com',
      countryCode: '+381',
      phone: '641112223',
      address: 'Narodnog Fronta 10',
      password: 'Password1',
      confirmPassword: 'Password1',
      agreeToTerms: true,
    });

    component.onSubmit();

    expect(registerService.register).toHaveBeenCalledWith({
      firstName: 'Milos',
      lastName: 'Milosevic',
      email: 'milos@example.com',
      phoneNumber: '+381641112223',
      address: 'Narodnog Fronta 10',
      password: 'Password1',
      confirmPassword: 'Password1',
      profilePicture: 'avatar.png',
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/login']);
  });
});
