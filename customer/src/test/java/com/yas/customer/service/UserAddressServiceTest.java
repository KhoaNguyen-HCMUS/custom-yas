package com.yas.customer.service;

import static com.yas.customer.util.SecurityContextUtils.setUpSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.commonlibrary.exception.AccessDeniedException;
import com.yas.commonlibrary.exception.NotFoundException;
import com.yas.customer.model.UserAddress;
import com.yas.customer.repository.UserAddressRepository;
import com.yas.customer.utils.Constants;
import com.yas.customer.viewmodel.address.ActiveAddressVm;
import com.yas.customer.viewmodel.address.AddressDetailVm;
import com.yas.customer.viewmodel.address.AddressPostVm;
import com.yas.customer.viewmodel.address.AddressVm;
import com.yas.customer.viewmodel.useraddress.UserAddressVm;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserAddressServiceTest {

    private UserAddressRepository userAddressRepository;
    private LocationService locationService;
    private UserAddressService userAddressService;

    @BeforeEach
    void setUp() {
        userAddressRepository = mock(UserAddressRepository.class);
        locationService = mock(LocationService.class);
        userAddressService = new UserAddressService(userAddressRepository, locationService);
    }

    @Test
    void getUserAddressList_whenAnonymousUser_thenThrowAccessDenied() {
        setUpSecurityContext("anonymousUser");

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            () -> userAddressService.getUserAddressList()
        );

        assertThat(exception.getMessage()).isEqualTo(Constants.ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void getUserAddressList_whenNormalUser_thenReturnSortedActiveAddresses() {
        setUpSecurityContext("user1");

        UserAddress inactive = UserAddress.builder()
            .id(1L)
            .userId("user1")
            .addressId(10L)
            .isActive(false)
            .build();

        UserAddress active = UserAddress.builder()
            .id(2L)
            .userId("user1")
            .addressId(11L)
            .isActive(true)
            .build();

        when(userAddressRepository.findAllByUserId("user1"))
            .thenReturn(List.of(inactive, active));

        AddressDetailVm detailInactive = AddressDetailVm.builder()
            .id(10L)
            .contactName("User Inactive")
            .phone("111")
            .addressLine1("Addr 10")
            .city("City")
            .zipCode("10000")
            .districtId(1L)
            .districtName("District 1")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("State")
            .countryId(3L)
            .countryName("Country")
            .build();

        AddressDetailVm detailActive = AddressDetailVm.builder()
            .id(11L)
            .contactName("User Active")
            .phone("222")
            .addressLine1("Addr 11")
            .city("City")
            .zipCode("20000")
            .districtId(4L)
            .districtName("District 2")
            .stateOrProvinceId(5L)
            .stateOrProvinceName("State")
            .countryId(6L)
            .countryName("Country")
            .build();

        when(locationService.getAddressesByIdList(List.of(10L, 11L)))
            .thenReturn(List.of(detailInactive, detailActive));

        List<ActiveAddressVm> result = userAddressService.getUserAddressList();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().isActive()).isTrue();
        assertThat(result.getFirst().id()).isEqualTo(11L);
    }

    @Test
    void getAddressDefault_whenAnonymousUser_thenThrowAccessDenied() {
        setUpSecurityContext("anonymousUser");

        assertThrows(
            AccessDeniedException.class,
            () -> userAddressService.getAddressDefault()
        );
    }

    @Test
    void getAddressDefault_whenAddressMissing_thenThrowNotFound() {
        setUpSecurityContext("user1");
        when(userAddressRepository.findByUserIdAndIsActiveTrue("user1"))
            .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> userAddressService.getAddressDefault()
        );

        assertThat(exception.getMessage()).isEqualTo("User address not found");
    }

    @Test
    void getAddressDefault_whenAddressExists_thenReturnDetail() {
        setUpSecurityContext("user1");
        UserAddress userAddress = UserAddress.builder()
            .id(1L)
            .userId("user1")
            .addressId(10L)
            .isActive(true)
            .build();

        when(userAddressRepository.findByUserIdAndIsActiveTrue("user1"))
            .thenReturn(Optional.of(userAddress));

        AddressDetailVm detailVm = AddressDetailVm.builder()
            .id(10L)
            .contactName("User")
            .phone("123")
            .addressLine1("Addr")
            .city("City")
            .zipCode("00000")
            .districtId(1L)
            .districtName("District")
            .stateOrProvinceId(2L)
            .stateOrProvinceName("State")
            .countryId(3L)
            .countryName("Country")
            .build();

        when(locationService.getAddressById(10L)).thenReturn(detailVm);

        AddressDetailVm result = userAddressService.getAddressDefault();

        assertThat(result).isEqualTo(detailVm);
    }

    @Test
    void createAddress_whenFirstAddress_thenMarkActive() {
        setUpSecurityContext("user1");
        when(userAddressRepository.findAllByUserId("user1"))
            .thenReturn(List.of());

        AddressPostVm postVm = new AddressPostVm(
            "John",
            "123",
            "Addr",
            "City",
            "00000",
            1L,
            2L,
            3L
        );

        AddressVm addressVm = AddressVm.builder()
            .id(10L)
            .contactName("John")
            .phone("123")
            .addressLine1("Addr")
            .city("City")
            .zipCode("00000")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        when(locationService.createAddress(postVm)).thenReturn(addressVm);

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        when(userAddressRepository.save(any(UserAddress.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserAddressVm vm = userAddressService.createAddress(postVm);

        verify(userAddressRepository).save(captor.capture());
        UserAddress saved = captor.getValue();
        assertThat(saved.getIsActive()).isTrue();
        assertThat(vm.isActive()).isTrue();
        assertThat(vm.addressGetVm().id()).isEqualTo(10L);
    }

    @Test
    void createAddress_whenExistingAddresses_thenMarkInactive() {
        setUpSecurityContext("user1");
        UserAddress existing = UserAddress.builder()
            .id(1L)
            .userId("user1")
            .addressId(5L)
            .isActive(true)
            .build();

        when(userAddressRepository.findAllByUserId("user1"))
            .thenReturn(List.of(existing));

        AddressPostVm postVm = new AddressPostVm(
            "John",
            "123",
            "Addr",
            "City",
            "00000",
            1L,
            2L,
            3L
        );

        AddressVm addressVm = AddressVm.builder()
            .id(10L)
            .contactName("John")
            .phone("123")
            .addressLine1("Addr")
            .city("City")
            .zipCode("00000")
            .districtId(1L)
            .stateOrProvinceId(2L)
            .countryId(3L)
            .build();

        when(locationService.createAddress(postVm)).thenReturn(addressVm);

        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        when(userAddressRepository.save(any(UserAddress.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        UserAddressVm vm = userAddressService.createAddress(postVm);

        verify(userAddressRepository).save(captor.capture());
        UserAddress saved = captor.getValue();
        assertThat(saved.getIsActive()).isFalse();
        assertThat(vm.isActive()).isFalse();
    }

    @Test
    void deleteAddress_whenAddressMissing_thenThrowNotFound() {
        setUpSecurityContext("user1");
        when(userAddressRepository.findOneByUserIdAndAddressId("user1", 10L))
            .thenReturn(null);

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> userAddressService.deleteAddress(10L)
        );

        assertThat(exception.getMessage()).isEqualTo("User address not found");
    }

    @Test
    void deleteAddress_whenAddressExists_thenDeleteInvoked() {
        setUpSecurityContext("user1");
        UserAddress userAddress = UserAddress.builder()
            .id(1L)
            .userId("user1")
            .addressId(10L)
            .isActive(true)
            .build();

        when(userAddressRepository.findOneByUserIdAndAddressId("user1", 10L))
            .thenReturn(userAddress);

        userAddressService.deleteAddress(10L);

        verify(userAddressRepository).delete(userAddress);
    }

    @Test
    void chooseDefaultAddress_whenCalled_thenOnlyOneActive() {
        setUpSecurityContext("user1");

        UserAddress address1 = UserAddress.builder()
            .id(1L)
            .userId("user1")
            .addressId(10L)
            .isActive(false)
            .build();

        UserAddress address2 = UserAddress.builder()
            .id(2L)
            .userId("user1")
            .addressId(11L)
            .isActive(true)
            .build();

        UserAddress address3 = UserAddress.builder()
            .id(3L)
            .userId("user1")
            .addressId(12L)
            .isActive(false)
            .build();

        List<UserAddress> userAddresses = List.of(address1, address2, address3);
        when(userAddressRepository.findAllByUserId("user1"))
            .thenReturn(userAddresses);

        userAddressService.chooseDefaultAddress(12L);

        ArgumentCaptor<List<UserAddress>> captor = ArgumentCaptor.forClass(List.class);
        verify(userAddressRepository).saveAll(captor.capture());
        List<UserAddress> savedList = captor.getValue();

        assertThat(savedList)
            .filteredOn(ua -> Boolean.TRUE.equals(ua.getIsActive()))
            .singleElement()
            .extracting(UserAddress::getAddressId)
            .isEqualTo(12L);
    }
}

