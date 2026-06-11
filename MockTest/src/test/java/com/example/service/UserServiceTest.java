package com.example.service;

import com.example.model.User;
import com.example.repository.UserRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private User testUser;

    @Before
    public void setUp() {
        testUser = new User(1, "John Doe", "john@example.com", true);
    }

    // ===== registerUser Tests =====
    @Test
    public void testRegisterUserSuccess() {
        User expectedUser = new User(1, "Jane Smith", "jane@example.com", true);
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        User result = userService.registerUser("Jane Smith", "jane@example.com");

        assertNotNull(result);
        assertEquals("Jane Smith", result.name());
        assertEquals("jane@example.com", result.email());
        assertTrue(result.active());
        verify(userRepository, times(1)).save(any(User.class));
        verify(emailService, times(1)).sendWelcomeEmail("jane@example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserWithEmptyName() {
        userService.registerUser("", "test@example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserWithNullName() {
        userService.registerUser(null, "test@example.com");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserWithInvalidEmail() {
        userService.registerUser("John", "invalid-email");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterUserWithNullEmail() {
        userService.registerUser("John", null);
    }

    // ===== getUserById Tests =====
    @Test
    public void testGetUserByIdSuccess() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1);

        assertNotNull(result);
        assertEquals(1, result.id());
        assertEquals("John Doe", result.name());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    public void testGetUserByIdNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        User result = userService.getUserById(999);

        assertNull(result);
        verify(userRepository, times(1)).findById(999);
    }

    // ===== getAllUsers Tests =====
    @Test
    public void testGetAllUsersSuccess() {
        List<User> userList = new ArrayList<>();
        userList.add(testUser);
        userList.add(new User(2, "Jane Doe", "jane@example.com", true));
        when(userRepository.findAll()).thenReturn(userList);

        List<User> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    public void testGetAllUsersEmpty() {
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        List<User> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    // ===== updateUser Tests =====
    @Test
    public void testUpdateUserSuccess() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        User updatedUser = new User(1, "John Updated", "john.updated@example.com", true);
        when(userRepository.update(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser(1, "John Updated", "john.updated@example.com");

        assertNotNull(result);
        assertEquals("John Updated", result.name());
        verify(userRepository).findById(1);
        verify(userRepository).update(any(User.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUpdateUserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());
        userService.updateUser(999, "Test", "test@example.com");
    }

    @Test
    public void testUpdateUserCapturesCorrectData() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));
        when(userRepository.update(any(User.class))).thenReturn(testUser);

        userService.updateUser(1, "Updated Name", "updated@example.com");

        verify(userRepository).update(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals("Updated Name", capturedUser.name());
        assertEquals("updated@example.com", capturedUser.email());
    }

    // ===== deactivateUser Tests =====
    @Test
    public void testDeactivateUserSuccess() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        boolean result = userService.deactivateUser(1);

        assertTrue(result);
        verify(userRepository).update(any(User.class));
        verify(emailService).sendGoodbyeEmail("john@example.com");
    }

    @Test
    public void testDeactivateUserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        boolean result = userService.deactivateUser(999);

        assertFalse(result);
        verify(userRepository, never()).update(any(User.class));
        verify(emailService, never()).sendGoodbyeEmail(anyString());
    }

    @Test
    public void testDeactivateUserSetsInactiveFlag() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        userService.deactivateUser(1);

        verify(userRepository).update(userCaptor.capture());
        assertFalse(userCaptor.getValue().active());
    }

    // ===== deleteUser Tests =====
    @Test
    public void testDeleteUserSuccess() {
        when(userRepository.deleteById(1)).thenReturn(true);

        boolean result = userService.deleteUser(1);

        assertTrue(result);
        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    public void testDeleteUserNotFound() {
        when(userRepository.deleteById(999)).thenReturn(false);

        boolean result = userService.deleteUser(999);

        assertFalse(result);
    }

    // ===== searchUsersByName Tests =====
    @Test
    public void testSearchUsersByNameSuccess() {
        List<User> searchResults = new ArrayList<>();
        searchResults.add(testUser);
        when(userRepository.findByName("John")).thenReturn(searchResults);

        List<User> result = userService.searchUsersByName("John");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("John Doe", result.get(0).name());
    }

    @Test
    public void testSearchUsersByNameEmpty() {
        when(userRepository.findByName("NonExistent")).thenReturn(new ArrayList<>());

        List<User> result = userService.searchUsersByName("NonExistent");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSearchUsersByNameEmptyInput() {
        userService.searchUsersByName("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSearchUsersByNameNullInput() {
        userService.searchUsersByName(null);
    }

    // ===== activateUser Tests =====
    @Test
    public void testActivateUserSuccess() {
        testUser = new User(1, "John Doe", "john@example.com", false);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        boolean result = userService.activateUser(1);

        assertTrue(result);
        verify(userRepository).update(any(User.class));
    }

    @Test
    public void testActivateUserNotFound() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        boolean result = userService.activateUser(999);

        assertFalse(result);
        verify(userRepository, never()).update(any(User.class));
    }

    @Test
    public void testActivateUserSetsActiveFlag() {
        testUser = new User(1, "John Doe", "john@example.com", false);
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        userService.activateUser(1);

        verify(userRepository).update(userCaptor.capture());
        assertTrue(userCaptor.getValue().active());
    }

    // ===== Mockito Annotation Tests =====
    @Test
    public void testVerifyMockInteractionOrder() {
        User expectedUser = new User(1, "Test", "test@example.com", true);
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        userService.registerUser("Test", "test@example.com");

        InOrder inOrder = inOrder(userRepository, emailService);
        inOrder.verify(userRepository).save(any(User.class));
        inOrder.verify(emailService).sendWelcomeEmail("test@example.com");
    }

    @Test
    public void testVerifyMethodNotCalled() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        userService.deactivateUser(1);

        verify(emailService, never()).sendGoodbyeEmail(anyString());
    }

    @Test
    public void testArgumentMatcherAny() {
        User expectedUser = new User(1, "Test", "test@example.com", true);
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        User result = userService.registerUser("Test", "test@example.com");

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }
}