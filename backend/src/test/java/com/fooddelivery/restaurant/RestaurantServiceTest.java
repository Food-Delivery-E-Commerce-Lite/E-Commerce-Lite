package com.fooddelivery.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {
    @Mock
    private RestaurantRepository restaurants;

    @Mock
    private CategoryRepository categories;

    @Mock
    private MenuItemRepository menuItems;

    @InjectMocks
    private RestaurantService service;

    @Test
    void merchantCannotUpdateRestaurantOwnedBySomeoneElse() {
        Restaurant restaurant = new Restaurant(2L, "Restaurant", null, "Address",
            LocalTime.of(8, 0), LocalTime.of(22, 0));
        when(restaurants.findById(10L)).thenReturn(Optional.of(restaurant));
        RestaurantDtos.RestaurantRequest update = new RestaurantDtos.RestaurantRequest(
            "Updated", null, "Address", null, null);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> service.updateRestaurant(1L, 10L, update));

        assertEquals("NOT_OWNER", exception.getCode());
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void publicRestaurantLookupHidesPendingRestaurants() {
        Restaurant restaurant = new Restaurant(2L, "Pending", null, "Address",
            LocalTime.of(8, 0), LocalTime.of(22, 0));
        when(restaurants.findById(10L)).thenReturn(Optional.of(restaurant));

        assertThrows(ResourceNotFoundException.class, () -> service.getPublicRestaurant(10L));
    }

    @Test
    void adminCannotMoveRestaurantBackToPending() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> service.updateStatus(10L, RestaurantStatus.PENDING));

        assertEquals("INVALID_RESTAURANT_STATUS", exception.getCode());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
