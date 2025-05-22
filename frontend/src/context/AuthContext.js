import React, { createContext, useState, useEffect } from 'react';
import axios from 'axios';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [auth, setAuth] = useState({
    isAuthenticated: false,
    token: null,
    user: null,
    loading: true
  });

  useEffect(() => {
    // Check for token in localStorage
    const token = localStorage.getItem('token');
    const username = localStorage.getItem('username');
    
    if (token) {
      setAuth({
        isAuthenticated: true,
        token,
        user: { username },
        loading: false
      });
      
      // Set axios default headers
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    } else {
      setAuth({
        isAuthenticated: false,
        token: null,
        user: null,
        loading: false
      });
    }
  }, []);

  // Login
  const login = async (username, password) => {
    try {
      const response = await axios.post('/api/auth/login', { username, password });
      const { token, username: user } = response.data;
      
      // Save to localStorage
      localStorage.setItem('token', token);
      localStorage.setItem('username', user);
      
      // Set axios default headers
      axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      
      setAuth({
        isAuthenticated: true,
        token,
        user: { username: user },
        loading: false
      });
      
      return { success: true };
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.message || 'Login failed' 
      };
    }
  };

  // Register
  const register = async (username, email, password) => {
    try {
      const response = await axios.post('/api/auth/register', { 
        username, 
        email, 
        password 
      });
      
      return { success: true, message: response.data.message };
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.message || 'Registration failed' 
      };
    }
  };

  // Logout
  const logout = () => {
    // Remove from localStorage
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    
    // Remove axios default headers
    delete axios.defaults.headers.common['Authorization'];
    
    setAuth({
      isAuthenticated: false,
      token: null,
      user: null,
      loading: false
    });
  };

  return (
    <AuthContext.Provider 
      value={{ 
        auth, 
        login, 
        register, 
        logout 
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};