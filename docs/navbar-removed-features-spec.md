# Navbar removed items vs spec (specifikacija.md)

This document maps the navbar items we removed (because they had no route) to the spec and current implementation.

## Passenger

| Removed item    | Required by spec? | Where it lives now |
|-----------------|-------------------|--------------------|
| **Favorite Routes** | Yes (2.4.3) | **Partially:** Add/remove favorite is on **Ride History** (star per row, `onFavoriteToggled`). "Order from favorite routes" (choose favorite when ordering so pickup/destination/stops pre-fill) is **not** in Order Ride yet – either add a picker there or a dedicated `/favorite-routes` page. |
| **Settings**   | No separate page  | **Profile** covers it: spec 2.3 says change password on profile page. Change password is on Profile (button → reset-password flow). |
| **Support**    | Yes (passengers can "kontaktirati support") | **Not implemented.** Panic modal has "Contact support" but it only logs; there is no support/chat page or flow. |

## Driver

| Removed item    | Required by spec? | Where it lives now |
|-----------------|-------------------|--------------------|
| **Settings**   | No separate page  | Same as passenger – Profile. |
| **Support**    | Yes (drivers can contact support) | Same as passenger – not implemented. |

## Admin

| Removed item           | Required by spec? | Where it lives now |
|------------------------|-------------------|--------------------|
| **Dashboard**          | Implicit (overview) | No dashboard page. Spec says admin sees ride state of any driver (2.13). |
| **Active Rides**       | Yes (2.13)        | **Not implemented.** Spec: "Administrator pregleda stanje vožnje koja trenutno traje, bilo kog vozača" – need a page to search by driver and see current ride state. |
| **Panic Notifications** | Yes (2.6.3)     | **Not implemented.** Panic button exists in ride-tracking (passenger/driver); admins get notification, but there is no admin page to **pregledati** (review) panic notifications. |
| **Live Support / Chat** | Yes (2.11)       | **Not implemented.** Spec: admins provide support via live chat 24/7. |
| **Drivers**            | Yes (2.12)       | **Not implemented.** Spec: admins can block drivers; no "Drivers" list/management page. Register Driver exists (`/register-driver`), but not list/block. |
| **Passengers**         | Yes (2.12)       | **Not implemented.** Spec: admins can block passengers; no list/management page. |
| **Reports**            | Yes (2.10)       | **Not implemented.** Spec: date-range reports with graphs (rides per day, km, money); admins can see for all drivers/passengers or one person. Ride history has date filters but no dedicated reports/charts page. |
| **All Notifications**  | Optional         | Not a separate spec section; could be part of panic/admin inbox. |
| **Settings**           | No separate page  | Profile covers password change. |

## Summary

- **We do need** (per spec) and **do not yet have** dedicated pages/routes for:
  - **Support** (passenger + driver): contact support / chat.
  - **Admin:** Active Rides (2.13), Panic Notifications (2.6.3), Live Support/Chat (2.11), Drivers list/block (2.12), Passengers list/block (2.12), Reports with graphs (2.10).
- **We do not need** a separate **Settings** route: spec puts change password on the profile page; implemented there.
- **Favorite Routes:** Spec requires (1) add/remove favorites on ride history – **done**; (2) order from favorites (pre-fill from a chosen favorite) – **not done** (can be added in Order Ride or via a dedicated Favorite Routes page).

When you implement any of the missing features above, add the corresponding route and navbar entry in `frontend/src/app/layout/config/navbar.config.ts`.
