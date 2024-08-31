import React, { useState, useEffect } from 'react';

//define structure of the book JSON
interface Book{ 
    title: string;
    level: 'N5' | 'N4' | 'N3' | 'N2' | 'N1';
}

const BookList: React.FC = () => { 
    const [books, setBooks] = useState<Book[]>([]);
    const [filteredBooks, setFilteredBooks] = useState<Book[]>([]);
    const [filter, setFilter] = useState<string>('All');

    useEffect(() => {
        //fetch json data from the file
        fetch('../books_by_jlpt.json')
            .then(response => response.json())
            .then(data => {
                //change json structure into array of books
                const allBooks = Object.keys(data).flatMap(level => 
                    data[level].map((title: string) => ({ title, level }))
                );
                setBooks(allBooks);
                setFilteredBooks(allBooks);
            })
            .catch(error => console.error('Error laoding the book data: ', error));
    }, []);

    //update the filtered books whenever the filter changes
    useEffect(() => {
        if(filter === 'All') {
            setFilteredBooks(books);
        }else{
            setFilteredBooks(books.filter(book => book.level === filter));
        }
    }, [filter, books]);

    return (
        <div className='container mx-auto p-4'>
            <h1 className='text-2xl font-bold mb-4'>Manga Books by JLPT</h1>
            <div className='mb-4'>
                <label htmlFor='filter' className='mr-2'>Filter by JLPT Level:</label>
                <select
                    id='filter'
                    className='p-2 border'
                    value={filter}
                    onChange={e => setFilter(e.target.value)}
                >
                    <option value="All">All</option>
                    <option value="N5">N5</option>
                    <option value="N4">N4</option>
                    <option value="N3">N3</option>
                    <option value="N2">N2</option>
                    <option value="N1">N1</option>
                </select>
            </div>
            <ul className='list-disc pl-5'>
                {filteredBooks.map((book, index) => (
                    <li key={index}>{book.title} ({book.level})</li>
                ))}
            </ul>
        </div>
    )
};

export default BookList;

